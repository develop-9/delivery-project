package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryGetQuery(
        UUID deliveryId,
        UUID requesterId,
        Role requesterRole
) {
    public static DeliveryGetQuery from(
            UUID deliveryId,
            UUID requesterId,
            Role requesterRole
    ){
        return new DeliveryGetQuery(
                deliveryId,
                requesterId,
                requesterRole
        );
    }
}
