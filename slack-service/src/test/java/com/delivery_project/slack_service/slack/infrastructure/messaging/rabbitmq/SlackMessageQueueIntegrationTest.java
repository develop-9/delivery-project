package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import com.delivery_project.slack_service.slack.application.command_service.SlackMessageCommandService;
import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import com.delivery_project.slack_service.slack.application.port.SlackMessageSender;
import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.infrastructure.config.SlackRabbitMqConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackMessageQueueIntegrationTest {

    private static final String HOST = "localhost";
    private static final int PORT = 5672;
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";

    @Test
    void 발송_실패_후_TTL_경과하면_Retry_Queue에서_Main_Queue로_복귀해_재시도된다() {

        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(HOST, PORT);

        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);

        assumeRabbitMqAvailable(connectionFactory);

        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            SlackRabbitMqConfig config = new SlackRabbitMqConfig();

            DirectExchange exchange = config.slackMessageExchange();
            Queue mainQueue = config.slackMessageMainQueue();
            Queue retryQueue = config.slackMessageRetryQueue();

            Binding mainBinding =
                    config.slackMessageMainBinding(mainQueue, exchange);

            Binding retryBinding =
                    config.slackMessageRetryBinding(retryQueue, exchange);

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
                    new RabbitTemplate(connectionFactory);

            rabbitTemplate.setMessageConverter(
                    new JacksonJsonMessageConverter()
            );

            SlackMessageQueuePublisherImpl publisher =
                    new SlackMessageQueuePublisherImpl(rabbitTemplate);

            SlackMessageSender sender =
                    mock(SlackMessageSender.class);

            SlackMessageCommandService commandService =
                    mock(SlackMessageCommandService.class);

            SlackMessageDuplicateGuard duplicateGuard =
                    mock(SlackMessageDuplicateGuard.class);

            String receiverSlackId = "U1234567890";
            String message = "통합 테스트 메시지";

            SlackMessage failedMessage = SlackMessage.create(
                    UUID.randomUUID(),
                    SenderType.SYSTEM,
                    UUID.randomUUID(),
                    receiverSlackId,
                    message
            );

            failedMessage.markAsFailed("일부러 실패시킴");

            SlackMessage retryMessage = SlackMessage.create(
                    UUID.randomUUID(),
                    SenderType.SYSTEM,
                    UUID.randomUUID(),
                    receiverSlackId,
                    message
            );

            retryMessage.prepareRetry();

            when(duplicateGuard.tryAcquire(any()))
                    .thenReturn("test-lock-token");

            when(sender.send(receiverSlackId, message))
                    .thenReturn(
                            SlackMessageSendResult.failed(
                                    "일부러 실패시킴"
                            )
                    )
                    .thenReturn(
                            SlackMessageSendResult.succeeded()
                    );

            when(commandService.markFailed(any(), any()))
                    .thenReturn(failedMessage);

            when(commandService.prepareRetry(any()))
                    .thenReturn(retryMessage);

            SlackMessageQueueConsumer consumer =
                    new SlackMessageQueueConsumer(
                            sender,
                            commandService,
                            publisher,
                            duplicateGuard
                    );

            // when: Main Queue에 최초 발행 후 수신 - 발송 실패 처리
            publisher.publish(
                    UUID.randomUUID(),
                    receiverSlackId,
                    message
            );

            SlackMessageQueuePayload firstPayload =
                    (SlackMessageQueuePayload)
                            rabbitTemplate.receiveAndConvert(
                                    SlackRabbitMqConfig.MAIN_QUEUE,
                                    5000
                            );

            assertThat(firstPayload)
                    .as("최초 발행한 메시지를 Main Queue에서 수신해야 한다")
                    .isNotNull();

            consumer.consume(firstPayload);

            // then: Retry Queue TTL 경과 후 Main Queue 복귀
            SlackMessageQueuePayload redeliveredPayload =
                    (SlackMessageQueuePayload)
                            rabbitTemplate.receiveAndConvert(
                                    SlackRabbitMqConfig.MAIN_QUEUE,
                                    8000
                            );

            assertThat(redeliveredPayload)
                    .as(
                            "Retry Queue의 TTL이 지나면 DLX를 통해 Main Queue로 복귀해야 한다"
                    )
                    .isNotNull();

            consumer.consume(redeliveredPayload);

            verify(sender, times(2))
                    .send(receiverSlackId, message);

            verify(commandService)
                    .markSent(any());

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