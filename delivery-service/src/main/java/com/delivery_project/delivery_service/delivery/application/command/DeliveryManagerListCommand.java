package com.delivery_project.delivery_service.delivery.application.command;

public record DeliveryManagerListCommand(
        int page,
        int size
) {

    public static DeliveryManagerListCommand of(
            int page,
            int size
    ) {
        return new DeliveryManagerListCommand(
                page,
                size
        );
    }
}