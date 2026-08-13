package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerActiveStatusResult;

import java.util.UUID;

public record DeliveryManagerActiveStatusResponse(
        UUID managerId,
        UUID userId,
        boolean active
) {
    public static DeliveryManagerActiveStatusResponse from(
            DeliveryManagerActiveStatusResult result
    ) {
        return new DeliveryManagerActiveStatusResponse(
                result.managerId(),
                result.userId(),
                result.active()
        );
    }
}
