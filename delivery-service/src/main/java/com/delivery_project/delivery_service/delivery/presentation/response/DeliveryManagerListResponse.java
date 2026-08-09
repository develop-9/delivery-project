package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerListResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerListResponse(
        UUID managerId,
        UUID userId,
        UUID hubId,
        DeliveryManagerType type,
        DeliveryManagerStatus status,
        Integer deliverySequence,
        Instant createdAt,
        Instant updatedAt
) {

    public static DeliveryManagerListResponse from(
            DeliveryManagerListResult result
    ) {
        return new DeliveryManagerListResponse(
                result.managerId(),
                result.userId(),
                result.hubId(),
                result.type(),
                result.status(),
                result.deliverySequence(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}