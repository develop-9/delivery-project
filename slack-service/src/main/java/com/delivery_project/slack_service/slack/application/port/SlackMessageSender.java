package com.delivery_project.slack_service.slack.application.port;

import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;

public interface SlackMessageSender {

    SlackMessageSendResult send(
            String receiverSlackId,
            String message
    );
}