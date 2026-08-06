package com.delivery_project.slack_service.slack.application.result;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageCreateResult(
        UUID id,
        SlackMessageStatus status,
        int retryCount,
        Instant createdAt
) {

    public static SlackMessageCreateResult from(SlackMessage slackMessage) {
        return new SlackMessageCreateResult(
                slackMessage.getId(),
                slackMessage.getStatus(),
                slackMessage.getRetryCount(),
                slackMessage.getCreatedAt()
        );
    }
}