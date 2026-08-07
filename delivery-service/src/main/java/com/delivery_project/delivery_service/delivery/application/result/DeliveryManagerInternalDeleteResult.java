package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerInternalDeleteResult(
        UUID managerId,
        UUID userId,
        Instant deletedAt
) {

    public static DeliveryManagerInternalDeleteResult from(
            DeliveryManager deliveryManager
    ) {
        return new DeliveryManagerInternalDeleteResult(
                deliveryManager.getId(),
                deliveryManager.getUserId(),
                deliveryManager.getDeletedAt()
        );
    }
}