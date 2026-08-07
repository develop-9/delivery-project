package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerDeleteResult(
        UUID managerId,
        Instant deletedAt
) {
    public static DeliveryManagerDeleteResult from(
            DeliveryManager deliveryManager
    ){
        return new DeliveryManagerDeleteResult(
                deliveryManager.getId(),
                deliveryManager.getDeletedAt()
        );
    }
}
