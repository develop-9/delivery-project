package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.util.UUID;

public record DeliveryStatusUpdateCommand(
        UUID deliveryId,
        DeliveryStatus status
) {
}
