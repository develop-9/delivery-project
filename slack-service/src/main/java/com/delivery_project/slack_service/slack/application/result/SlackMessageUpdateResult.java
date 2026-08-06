package com.delivery_project.slack_service.slack.application.result;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageUpdateResult(
        UUID id,
        String message,
        SlackMessageStatus status,
        Instant updatedAt
) {

    public static SlackMessageUpdateResult from(SlackMessage slackMessage) {
        return new SlackMessageUpdateResult(
                slackMessage.getId(),
                slackMessage.getMessage(),
                slackMessage.getStatus(),
                slackMessage.getUpdatedAt()
        );
    }
}