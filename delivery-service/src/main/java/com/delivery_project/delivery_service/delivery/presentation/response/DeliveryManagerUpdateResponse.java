package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerUpdateResponse(
        UUID managerId,
        UUID hubId,
        DeliveryManagerType type,
        DeliveryManagerStatus status,
        Integer deliverySequence,
        Instant updatedAt
) {
    public static DeliveryManagerUpdateResponse from(
            DeliveryManagerUpdateResult result
    ){
        return new DeliveryManagerUpdateResponse(
                result.managerId(),
                result.hubId(),
                result.type(),
                result.status(),
                result.deliverySequence(),
                result.updatedAt()
        );
    }
}
