package com.delivery_project.slack_service.slack.infrastructure.external.slack;

public record SlackPostMessageResponse(
        boolean ok,
        String error
) {
}