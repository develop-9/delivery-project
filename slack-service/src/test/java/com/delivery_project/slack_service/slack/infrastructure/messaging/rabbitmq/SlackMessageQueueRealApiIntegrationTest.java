package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import com.delivery_project.slack_service.slack.application.command_service.SlackMessageCommandService;
import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import com.delivery_project.slack_service.slack.infrastructure.config.SlackRabbitMqConfig;
import com.delivery_project.slack_service.slack.infrastructure.external.slack.SlackMessageSenderImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RabbitMQ → Slack 실제 API E2E 테스트")
class SlackMessageQueueRealApiIntegrationTest {

    private static final String HOST = "localhost";
    private static final int PORT = 5672;
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";

    @Test
    @DisplayName("RabbitMQ 메시지가 Consumer를 거쳐 실제 Slack DM으로 발송된다")
    void rabbitMq_to_realSlackDm() {

        String botToken =
                System.getenv("SLACK_BOT_TOKEN");

        String receiverSlackId =
                System.getenv("SLACK_TEST_RECEIVER_ID");

        String slackBaseUrl =
                System.getenv("SLACK_BASE_URL");

        Assumptions.assumeTrue(
                botToken != null && !botToken.isBlank(),
                "SLACK_BOT_TOKEN 환경변수가 없어 테스트를 건너뜁니다."
        );

        Assumptions.assumeTrue(
                receiverSlackId != null && !receiverSlackId.isBlank(),
                "SLACK_TEST_RECEIVER_ID 환경변수가 없어 테스트를 건너뜁니다."
        );

        if (slackBaseUrl == null || slackBaseUrl.isBlank()) {
            slackBaseUrl = "https://slack.com/api";
        }

        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(
                        HOST,
                        PORT
                );

        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);

        assumeRabbitMqAvailable(connectionFactory);

        try {
            RabbitAdmin rabbitAdmin =
                    new RabbitAdmin(connectionFactory);

            SlackRabbitMqConfig config =
                    new SlackRabbitMqConfig();

            DirectExchange exchange =
                    config.slackMessageExchange();

            Queue mainQueue =
                    config.slackMessageMainQueue();

            Queue retryQueue =
                    config.slackMessageRetryQueue();

            Binding mainBinding =
                    config.slackMessageMainBinding(
                            mainQueue,
                            exchange
                    );

            Binding retryBinding =
                    config.slackMessageRetryBinding(
                            retryQueue,
                            exchange
                    );

            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareQueue(mainQueue);
            rabbitAdmin.declareQueue(retryQueue);
            rabbitAdmin.declareBinding(mainBinding);
            rabbitAdmin.declareBinding(retryBinding);

            rabbitAdmin.purgeQueue(
                    SlackRabbitMqConfig.MAIN_QUEUE,
                    false
            );

            rabbitAdmin.purgeQueue(
                    SlackRabbitMqConfig.RETRY_QUEUE,
                    false
            );

            RabbitTemplate rabbitTemplate =
                    new RabbitTemplate(
                            connectionFactory
                    );

            JacksonJsonMessageConverter messageConverter =
                    new JacksonJsonMessageConverter(
                            "com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq"
                    );

            rabbitTemplate.setMessageConverter(
                    messageConverter
            );

            SlackMessageQueuePublisherImpl publisher =
                    new SlackMessageQueuePublisherImpl(
                            rabbitTemplate
                    );

            SlackMessageSenderImpl realSlackSender =
                    new SlackMessageSenderImpl(
                            slackBaseUrl,
                            botToken
                    );

            SlackMessageCommandService commandService =
                    mock(
                            SlackMessageCommandService.class
                    );

            SlackMessageDuplicateGuard duplicateGuard =
                    mock(
                            SlackMessageDuplicateGuard.class
                    );

            when(
                    duplicateGuard.tryAcquire(any())
            ).thenReturn(
                    "real-api-test-lock-token"
            );

            UUID slackMessageId =
                    UUID.randomUUID();

            String message =
                    """
                    [배송 프로젝트 최종 E2E 테스트]

                    RabbitMQ
                    → Consumer
                    → Slack API
                    → 실제 DM

                    테스트 시각: %s
                    """.formatted(
                            Instant.now()
                    );

            SlackMessageQueueConsumer consumer =
                    new SlackMessageQueueConsumer(
                            realSlackSender,
                            commandService,
                            publisher,
                            duplicateGuard
                    );

            publisher.publish(
                    slackMessageId,
                    receiverSlackId,
                    message
            );

            SlackMessageQueuePayload payload =
                    (SlackMessageQueuePayload)
                            rabbitTemplate.receiveAndConvert(
                                    SlackRabbitMqConfig.MAIN_QUEUE,
                                    5000
                            );

            assertThat(payload)
                    .as("RabbitMQ Main Queue에서 메시지를 수신해야 한다")
                    .isNotNull();

            assertThat(
                    payload.slackMessageId()
            ).isEqualTo(
                    slackMessageId
            );

            consumer.consume(
                    payload
            );

            verify(commandService)
                    .markSent(
                            slackMessageId
                    );

            verify(duplicateGuard)
                    .release(
                            slackMessageId,
                            "real-api-test-lock-token"
                    );

        } finally {
            connectionFactory.destroy();
        }
    }

    private void assumeRabbitMqAvailable(
            CachingConnectionFactory connectionFactory
    ) {
        try {
            Connection connection =
                    connectionFactory.createConnection();

            connection.close();

        } catch (Exception exception) {

            connectionFactory.destroy();

            Assumptions.abort(
                    "RabbitMQ("
                            + HOST
                            + ":"
                            + PORT
                            + ")에 연결할 수 없어 테스트를 건너뜁니다: "
                            + exception.getMessage()
            );
        }
    }
}