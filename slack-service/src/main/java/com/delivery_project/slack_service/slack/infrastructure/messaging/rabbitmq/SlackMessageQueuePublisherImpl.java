package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import com.delivery_project.slack_service.slack.application.port.SlackMessageQueuePublisher;
import com.delivery_project.slack_service.slack.infrastructure.config.SlackRabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlackMessageQueuePublisherImpl
        implements SlackMessageQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(
            UUID slackMessageId,
            String receiverSlackId,
            String message
    ) {
        SlackMessageQueuePayload payload =
                new SlackMessageQueuePayload(
                        slackMessageId,
                        receiverSlackId,
                        message
                );

        rabbitTemplate.convertAndSend(
                SlackRabbitMqConfig.EXCHANGE,
                SlackRabbitMqConfig.MAIN_ROUTING_KEY,
                payload
        );
    }

    @Override
    public void publishRetry(
            UUID slackMessageId,
            String receiverSlackId,
            String message
    ) {
        SlackMessageQueuePayload payload =
                new SlackMessageQueuePayload(
                        slackMessageId,
                        receiverSlackId,
                        message
                );

        rabbitTemplate.convertAndSend(
                SlackRabbitMqConfig.EXCHANGE,
                SlackRabbitMqConfig.RETRY_ROUTING_KEY,
                payload
        );
    }
}