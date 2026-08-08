package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.util.UUID;

public record UserSlackResponse(
        UUID userId,
        String slackId
) {
}
