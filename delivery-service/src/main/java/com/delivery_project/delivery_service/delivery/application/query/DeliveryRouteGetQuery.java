package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryRouteGetQuery(
        UUID routeId,
        UUID requesterId,
        Role requesterRole
) {
    public static DeliveryRouteGetQuery from(
            UUID routeId,
            UUID requesterId,
            Role requesterRole
    ){
        return new DeliveryRouteGetQuery(
                routeId,
                requesterId,
                requesterRole
        );
    }
}
