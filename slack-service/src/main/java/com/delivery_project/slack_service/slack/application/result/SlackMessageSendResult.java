package com.delivery_project.slack_service.slack.application.result;

public record SlackMessageSendResult(
        boolean success,
        String errorMessage
) {

    public static SlackMessageSendResult succeeded() {
        return new SlackMessageSendResult(
                true,
                null
        );
    }

    public static SlackMessageSendResult failed(String errorMessage) {
        return new SlackMessageSendResult(
                false,
                errorMessage
        );
    }
}