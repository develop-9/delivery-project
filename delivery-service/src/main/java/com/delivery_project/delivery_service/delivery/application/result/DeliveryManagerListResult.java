package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerListResult(
        UUID managerId,
        UUID userId,
        UUID hubId,
        DeliveryManagerType type,
        DeliveryManagerStatus status,
        Integer deliverySequence,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryManagerListResult from(
            DeliveryManager deliveryManager
    ){
        return new DeliveryManagerListResult(
                deliveryManager.getId(),
                deliveryManager.getUserId(),
                deliveryManager.getHubId(),
                deliveryManager.getType(),
                deliveryManager.getStatus(),
                deliveryManager.getDeliverySequence(),
                deliveryManager.getCreatedAt(),
                deliveryManager.getUpdatedAt()
        );
    }
}
