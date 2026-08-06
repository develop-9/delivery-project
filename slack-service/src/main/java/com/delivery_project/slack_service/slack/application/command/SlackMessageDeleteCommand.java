package com.delivery_project.slack_service.slack.application.command;

import java.util.UUID;

public record SlackMessageDeleteCommand(
        UUID slackMessageId,
        UUID deletedBy
) {

    public static SlackMessageDeleteCommand of(
            UUID slackMessageId,
            UUID deletedBy
    ) {
        return new SlackMessageDeleteCommand(
                slackMessageId,
                deletedBy
        );
    }
}