package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryManagerDeleteCommand(
        UUID managerId,
        UUID deletedBy
) {

    public static DeliveryManagerDeleteCommand of(
            UUID managerId,
            UUID deletedBy
    ) {
        return new DeliveryManagerDeleteCommand(
                managerId,
                deletedBy
        );
    }
}