package com.delivery_project.slack_service.slack.presentation.response;

import com.delivery_project.slack_service.slack.application.result.SlackInternalMessageCreateResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record SlackInternalMessageCreateResponse(
        UUID slackMessageId,
        UUID receiverUserId,
        SlackMessageStatus status,
        int retryCount,
        Instant requestedAt
) {

    public static SlackInternalMessageCreateResponse from(
            SlackInternalMessageCreateResult result
    ) {
        return new SlackInternalMessageCreateResponse(
                result.slackMessageId(),
                result.receiverUserId(),
                result.status(),
                result.retryCount(),
                result.requestedAt()
        );
    }
}