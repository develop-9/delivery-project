package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryListQuery(
        UUID orderId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID destinationHubId,
        UUID companyDeliveryManagerId,
        int page,
        int size,
        String sortBy,
        String direction,
        UUID requesterId,
        Role requesterRole
) {

    public static DeliveryListQuery of(
            UUID orderId,
            DeliveryStatus status,
            UUID departureHubId,
            UUID destinationHubId,
            UUID companyDeliveryManagerId,
            int page,
            int size,
            String sortBy,
            String direction,
            UUID requesterId,
            Role requesterRole
    ) {
        return new DeliveryListQuery(
                orderId,
                status,
                departureHubId,
                destinationHubId,
                companyDeliveryManagerId,
                page,
                size,
                sortBy,
                direction,
                requesterId,
                requesterRole
        );
    }
}