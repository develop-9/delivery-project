package com.delivery_project.delivery_service.delivery.application.query;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

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
        String direction
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
            String direction
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
                direction
        );
    }
}