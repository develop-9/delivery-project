package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerDeactivateCommand(
        UUID userId
) {
    public static DeliveryManagerDeactivateCommand from(
            UUID userId
    ){
        return new DeliveryManagerDeactivateCommand(userId);
    }
}
