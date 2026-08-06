package com.delivery_project.slack_service.slack.presentation.response;

import com.delivery_project.slack_service.slack.application.result.SlackMessageDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record SlackMessageDeleteResponse(
        UUID slackMessageId,
        Instant deletedAt
) {

    public static SlackMessageDeleteResponse from(
            SlackMessageDeleteResult result
    ) {
        return new SlackMessageDeleteResponse(
                result.slackMessageId(),
                result.deletedAt()
        );
    }
}