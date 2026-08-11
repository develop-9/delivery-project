package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryUpdateResponse(
        UUID deliveryId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        DeliveryStatus status,
        Instant updatedAt
) {
    public static DeliveryUpdateResponse from(
            DeliveryUpdateResult result
    ){
        return new DeliveryUpdateResponse(
                result.deliveryId(),
                result.deliveryAddress(),
                result.receiverName(),
                result.receiverSlackId(),
                result.status(),
                result.updatedAt()
        );
    }
}
