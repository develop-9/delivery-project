package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.util.UUID;

public record DeliveryCreateResult(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status,
        int routeCount
) {
}
