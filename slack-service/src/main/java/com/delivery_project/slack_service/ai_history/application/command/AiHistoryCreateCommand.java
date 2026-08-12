package com.delivery_project.slack_service.ai_history.application.command;

import java.util.UUID;

public record AiHistoryCreateCommand(
        UUID orderId
) {
}