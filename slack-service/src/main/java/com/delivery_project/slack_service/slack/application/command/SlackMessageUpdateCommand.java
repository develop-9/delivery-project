package com.delivery_project.slack_service.slack.application.command;

import java.util.UUID;

public record SlackMessageUpdateCommand(
        UUID slackMessageId,
        String message
) {
}