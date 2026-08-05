package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerGetMyCommand(
        UUID userId
) {

    public static DeliveryManagerGetMyCommand from(
            UUID userId
    ) {
        return new DeliveryManagerGetMyCommand(userId);
    }
}