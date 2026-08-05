package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerInternalDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerInternalDeleteResponse(
        UUID managerId,
        UUID userId,
        Instant deletedAt
) {

    public static DeliveryManagerInternalDeleteResponse from(
            DeliveryManagerInternalDeleteResult result
    ) {
        return new DeliveryManagerInternalDeleteResponse(
                result.managerId(),
                result.userId(),
                result.deletedAt()
        );
    }
}