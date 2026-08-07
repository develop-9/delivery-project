package com.delivery_project.slack_service.slack.presentation.response;

import com.delivery_project.slack_service.slack.application.result.SlackMessageQueryResult;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageQueryResponse(
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

    public static SlackMessageQueryResponse from(
            SlackMessageQueryResult result
    ) {
        return new SlackMessageQueryResponse(
                result.id(),
                result.senderUserId(),
                result.senderType(),
                result.receiverUserId(),
                result.receiverSlackId(),
                result.message(),
                result.status(),
                result.sentAt(),
                result.retryCount(),
                result.failureMessage(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}