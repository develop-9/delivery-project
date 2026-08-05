package com.delivery_project.slack_service.domain.slack.dto.response;

import com.delivery_project.slack_service.domain.slack.entity.SenderType;
import com.delivery_project.slack_service.domain.slack.entity.SlackMessage;
import com.delivery_project.slack_service.domain.slack.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageResponse(
        UUID id,
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
        Instant updatedAt
) {

    public static SlackMessageResponse from(SlackMessage slackMessage) {
        return new SlackMessageResponse(
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
                slackMessage.getUpdatedAt()
        );
    }
}