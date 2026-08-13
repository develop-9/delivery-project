package com.delivery_project.slack_service.slack.application.result;

import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageQueryResult(
        UUID slackMessageId,
        UUID senderUserId,
        SenderType senderType,
        UUID receiverUserId,
        String receiverSlackId,
        String message,
        SlackMessageStatus status,
        Instant sentAt,
        int retryCount,
        String failureMessage,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    public static SlackMessageQueryResult from(
            SlackMessage slackMessage
    ) {
        return new SlackMessageQueryResult(
                slackMessage.getId(),
                slackMessage.getSenderUserId(),
                slackMessage.getSenderType(),
                slackMessage.getReceiverUserId(),
                slackMessage.getReceiverSlackId(),
                slackMessage.getMessage(),
                slackMessage.getStatus(),
                slackMessage.getSentAt(),
                slackMessage.getRetryCount(),
                slackMessage.getFailureMessage(),
                slackMessage.getCreatedAt(),
                slackMessage.getCreatedBy(),
                slackMessage.getUpdatedAt(),
                slackMessage.getUpdatedBy()
        );
    }
}