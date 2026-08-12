package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryListResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryListResponse(
        UUID deliveryId,
        UUID orderId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        DeliveryStatus status,
        UUID companyDeliveryManagerId,
        Instant createdAt
) {
    public static DeliveryListResponse from(
            DeliveryListResult result
    ){
        return new DeliveryListResponse(
                result.deliveryId(),
                result.orderId(),
                result.departureHubId(),
                result.destinationHubId(),
                result.deliveryAddress(),
                result.receiverName(),
                result.status(),
                result.companyDeliveryManagerId(),
                result.createdAt()
        );
    }
}
