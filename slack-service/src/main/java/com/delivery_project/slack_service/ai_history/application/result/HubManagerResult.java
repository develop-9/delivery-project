package com.delivery_project.slack_service.ai_history.application.result;

import java.util.UUID;

public record HubManagerResult(
        UUID userId,
        String name,
        String role,
        UUID hubId
) {
}