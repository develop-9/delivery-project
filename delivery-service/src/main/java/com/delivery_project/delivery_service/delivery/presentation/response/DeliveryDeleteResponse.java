package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record DeliveryDeleteResponse(
        UUID deliveryId,
        Instant deletedAt
) {
    public static DeliveryDeleteResponse from(
            DeliveryDeleteResult result
    ){
        return new DeliveryDeleteResponse(
                result.deliveryId(),
                result.deletedAt()
        );
    }
}
