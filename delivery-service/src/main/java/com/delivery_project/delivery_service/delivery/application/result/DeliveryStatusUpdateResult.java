package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStatusUpdateResult(
        UUID deliveryId,
        DeliveryStatus previousStatus,
        DeliveryStatus status,
        UUID companyDeliveryManagerId,
        Instant updatedAt
) {
}