package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerListQuery(
        int page,
        int size,
        UUID requesterId,
        Role requesterRole
) {

    public static DeliveryManagerListQuery from(
            int page,
            int size,
            UUID requesterId,
            Role requesterRole
    ) {
        return new DeliveryManagerListQuery(
                page,
                size,
                requesterId,
                requesterRole
        );
    }
}