package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryUpdateResult(
        UUID deliveryId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        DeliveryStatus status,
        Instant updatedAt
) {
    public static DeliveryUpdateResult from(
            Delivery delivery
    ){
        return new DeliveryUpdateResult(
                delivery.getId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getStatus(),
                delivery.getUpdatedAt()
        );
    }
}
