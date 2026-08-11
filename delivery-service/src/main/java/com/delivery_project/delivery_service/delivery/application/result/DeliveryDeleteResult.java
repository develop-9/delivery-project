package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;

import java.time.Instant;
import java.util.UUID;

public record DeliveryDeleteResult(
        UUID deliveryId,
        Instant deletedAt
) {
    public static DeliveryDeleteResult from(
            Delivery delivery
    ) {
        return new DeliveryDeleteResult(
                delivery.getId(),
                delivery.getDeletedAt()
        );
    }
}
