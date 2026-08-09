package com.delivery_project.delivery_service.delivery.application.command;

import java.util.UUID;

public record DeliveryDeleteCommand(
        UUID deliveryId,
        UUID deletedBy
) {
}
