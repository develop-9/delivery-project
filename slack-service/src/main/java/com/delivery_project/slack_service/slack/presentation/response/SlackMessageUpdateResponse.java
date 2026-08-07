package com.delivery_project.slack_service.slack.presentation.response;

import com.delivery_project.slack_service.slack.application.result.SlackMessageUpdateResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageUpdateResponse(
        UUID id,
        String message,
        SlackMessageStatus status,
        Instant updatedAt
) {

    public static SlackMessageUpdateResponse from(
            SlackMessageUpdateResult result
    ) {
        return new SlackMessageUpdateResponse(
                result.id(),
                result.message(),
                result.status(),
                result.updatedAt()
        );
    }
}