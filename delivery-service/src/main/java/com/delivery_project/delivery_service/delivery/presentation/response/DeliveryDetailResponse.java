package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryDetailResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.util.UUID;

public record DeliveryDetailResponse(
        UUID deliveryId,
        UUID orderId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        DeliveryStatus status,
        UUID companyDeliveryManagerId
) {
    public static DeliveryDetailResponse from(
            DeliveryDetailResult result
    ) {
        return new DeliveryDetailResponse(
                result.deliveryId(),
                result.orderId(),
                result.departureHubId(),
                result.destinationHubId(),
                result.deliveryAddress(),
                result.receiverName(),
                result.receiverSlackId(),
                result.status(),
                result.companyDeliveryManagerId()
        );
    }
}
