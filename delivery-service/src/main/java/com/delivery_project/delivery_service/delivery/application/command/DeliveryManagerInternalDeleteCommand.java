package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerInternalDeleteCommand(
        UUID userId
) {

    public static DeliveryManagerInternalDeleteCommand from(
            UUID userId
    ) {
        return new DeliveryManagerInternalDeleteCommand(userId);
    }
}