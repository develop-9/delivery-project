package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import java.io.Serializable;
import java.util.UUID;

public record SlackMessageQueuePayload(
        UUID slackMessageId,
        String receiverSlackId,
        String message
) implements Serializable {
}