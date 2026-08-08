package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStatusUpdateResponse(
        UUID deliveryId,
        DeliveryStatus previousStatus,
        DeliveryStatus status,
        UUID companyDeliveryManagerId,
        Instant updatedAt
) {
    public static DeliveryStatusUpdateResponse from(
            DeliveryStatusUpdateResult result
    ){
        return new DeliveryStatusUpdateResponse(
                result.deliveryId(),
                result.previousStatus(),
                result.status(),
                result.companyDeliveryManagerId(),
                result.updatedAt()
        );
    }
}
