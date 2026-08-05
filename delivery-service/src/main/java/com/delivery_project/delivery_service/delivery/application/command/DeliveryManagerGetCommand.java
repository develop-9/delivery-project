package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerGetCommand(
        UUID managerId
) {

    public static DeliveryManagerGetCommand from(
            UUID managerId
    ) {
        return new DeliveryManagerGetCommand(managerId);
    }
}