package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record DeliveryManagerDeleteResponse(
        UUID managerId,
        Instant deletedAt
) {
    public static DeliveryManagerDeleteResponse from(
            DeliveryManagerDeleteResult result
    ){
        return new DeliveryManagerDeleteResponse(
                result.managerId(),
                result.deletedAt()
        );
    }
}
