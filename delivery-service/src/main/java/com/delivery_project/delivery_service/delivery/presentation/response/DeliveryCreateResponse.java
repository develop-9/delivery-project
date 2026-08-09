package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.util.UUID;

public record DeliveryCreateResponse(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status,
        int routeCount
) {
    public static DeliveryCreateResponse from(
            DeliveryCreateResult result
    ){
        return new DeliveryCreateResponse(
                result.deliveryId(),
                result.orderId(),
                result.status(),
                result.routeCount()
        );
    }
}
