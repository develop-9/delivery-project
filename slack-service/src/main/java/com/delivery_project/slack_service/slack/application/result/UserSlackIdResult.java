package com.delivery_project.slack_service.slack.application.result;

import java.util.UUID;

public record UserSlackIdResult(
        UUID userId,
        String slackId
) {
}