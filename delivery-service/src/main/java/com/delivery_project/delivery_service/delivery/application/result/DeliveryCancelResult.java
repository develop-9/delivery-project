package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCancelResult(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status,
        Instant updatedAt
) {
}
