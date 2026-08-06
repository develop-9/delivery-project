package com.delivery_project.slack_service.slack.application.result;

public record SlackMessageSendResult(
        boolean success
) {

    public static SlackMessageSendResult succeeded() {
        return new SlackMessageSendResult(true);
    }

    public static SlackMessageSendResult failed() {
        return new SlackMessageSendResult(false);
    }
}