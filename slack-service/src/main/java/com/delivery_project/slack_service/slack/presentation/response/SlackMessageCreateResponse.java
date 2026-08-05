package com.delivery_project.slack_service.slack.presentation.response;

import com.delivery_project.slack_service.slack.application.result.SlackMessageCreateResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageCreateResponse(
        UUID id,
        SlackMessageStatus status,
        int retryCount,
        Instant createdAt
) {

    public static SlackMessageCreateResponse from(
            SlackMessageCreateResult result
    ) {
        return new SlackMessageCreateResponse(
                result.id(),
                result.status(),
                result.retryCount(),
                result.createdAt()
        );
    }
}