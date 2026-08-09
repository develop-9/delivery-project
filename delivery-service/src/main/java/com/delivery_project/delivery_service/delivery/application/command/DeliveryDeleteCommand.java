package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryDeleteCommand(
        UUID deliveryId,
        UUID deletedBy,
        Role requesterRole
) {
}
