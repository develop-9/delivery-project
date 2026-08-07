package com.delivery_project.slack_service.slack.application.result;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageDeleteResult(
        UUID slackMessageId,
        Instant deletedAt
) {

    public static SlackMessageDeleteResult from(
            SlackMessage slackMessage
    ) {
        return new SlackMessageDeleteResult(
                slackMessage.getId(),
                slackMessage.getDeletedAt()
        );
    }
}