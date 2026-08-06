package com.delivery_project.slack_service.slack.application.command;

import java.time.Instant;
import java.util.UUID;

public record SlackInternalMessageCreateCommand(
        UUID receiverUserId,
        UUID orderId,
        Instant finalDispatchDeadline
) {
}