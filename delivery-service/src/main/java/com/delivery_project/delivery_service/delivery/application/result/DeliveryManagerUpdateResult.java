package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerUpdateResult(
        UUID managerId,
        UUID hubId,
        DeliveryManagerType type,
        DeliveryManagerStatus status,
        Integer deliverySequence,
        Instant updatedAt
) {
    public static DeliveryManagerUpdateResult from(
            DeliveryManager deliveryManager
    ){
        return new DeliveryManagerUpdateResult(
                deliveryManager.getId(),
                deliveryManager.getHubId(),
                deliveryManager.getType(),
                deliveryManager.getStatus(),
                deliveryManager.getDeliverySequence(),
                deliveryManager.getUpdatedAt()
        );
    }
}
