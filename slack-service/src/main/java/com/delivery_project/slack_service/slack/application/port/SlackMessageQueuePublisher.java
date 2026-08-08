package com.delivery_project.slack_service.slack.application.port;

import java.util.UUID;

public interface SlackMessageQueuePublisher {

    void publish(
            UUID slackMessageId,
            String receiverSlackId,
            String message
    );

    void publishRetry(
            UUID slackMessageId,
            String receiverSlackId,
            String message
    );
}