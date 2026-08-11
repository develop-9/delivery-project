package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;

import java.util.UUID;

public record DeliveryManagerActiveStatusResult(
        UUID managerId,
        UUID userId,
        boolean active
) {
    public static DeliveryManagerActiveStatusResult from(
            DeliveryManager deliveryManager
    ){
        return new DeliveryManagerActiveStatusResult(
                deliveryManager.getId(),
                deliveryManager.getUserId(),
                deliveryManager.isActive()
        );
    }
}
