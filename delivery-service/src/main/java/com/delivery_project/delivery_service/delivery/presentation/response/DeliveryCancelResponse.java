package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryCancelResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCancelResponse(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status,
        Instant updatedAt
) {

    public static DeliveryCancelResponse from(
            DeliveryCancelResult result
    ) {
        return new DeliveryCancelResponse(
                result.deliveryId(),
                result.orderId(),
                result.status(),
                result.updatedAt()
        );
    }
}