package com.delivery_project.slack_service.slack.application.result;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackInternalMessageCreateResult(
        UUID slackMessageId,
        UUID receiverUserId,
        SlackMessageStatus status,
        int retryCount,
        Instant createdAt
) {

    public static SlackInternalMessageCreateResult from(
            SlackMessage slackMessage
    ) {
        return new SlackInternalMessageCreateResult(
                slackMessage.getId(),
                slackMessage.getReceiverUserId(),
                slackMessage.getStatus(),
                slackMessage.getRetryCount(),
                slackMessage.getCreatedAt()
        );
    }
}