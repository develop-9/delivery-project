package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerReactivateCommand(
        UUID userId
) {
    public static DeliveryManagerReactivateCommand from(
            UUID userId
    ){
        return new DeliveryManagerReactivateCommand(userId);
    }
}
