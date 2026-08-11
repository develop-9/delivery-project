package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerGetQuery(
        UUID managerId,
        UUID requesterId,
        Role requesterRole
) {
    public static DeliveryManagerGetQuery from(
            UUID managerId,
            UUID requesterId,
            Role requesterRole
    ) {
        return new DeliveryManagerGetQuery(
                managerId,
                requesterId,
                requesterRole
        );
    }
}