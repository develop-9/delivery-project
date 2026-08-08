package com.delivery_project.slack_service.slack.infrastructure.external.slack;

public record SlackPostMessageRequest(
        String channel,
        String text
) {
}