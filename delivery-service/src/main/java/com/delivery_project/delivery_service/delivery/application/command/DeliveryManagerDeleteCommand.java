package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerDeleteCommand(
        UUID managerId,
        UUID deletedBy,
        Role requesterRole
) {

    public static DeliveryManagerDeleteCommand of(
            UUID managerId,
            UUID deletedBy,
            Role requesterRole
    ) {
        return new DeliveryManagerDeleteCommand(
                managerId,
                deletedBy,
                requesterRole
        );
    }
}