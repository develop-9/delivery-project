package com.delivery_project.slack_service.slack.application.command;

import com.delivery_project.slack_service.slack.domain.entity.SenderType;

import java.util.UUID;

public record SlackMessageCreateCommand(
        UUID senderUserId,
        SenderType senderType,
        UUID receiverUserId,
        String receiverSlackId,
        String message
) {
}