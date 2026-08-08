package com.delivery_project.delivery_service.delivery.application.result;

import java.util.UUID;

public record ReceiverInfo(
        UUID userId,
        String name,
        String slackId
) {
}
