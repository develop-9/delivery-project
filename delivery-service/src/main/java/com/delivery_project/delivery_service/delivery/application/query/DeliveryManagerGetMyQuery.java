package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerGetMyQuery(
        UUID userId,
        Role requesterRole
) {

    public static DeliveryManagerGetMyQuery from(
            UUID userId,
            Role requesterRole
    ) {
        return new DeliveryManagerGetMyQuery(
                userId,
                requesterRole);
    }
}