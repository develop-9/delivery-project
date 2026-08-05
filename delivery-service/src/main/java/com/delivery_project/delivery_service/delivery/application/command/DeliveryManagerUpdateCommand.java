package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.util.UUID;

public record DeliveryManagerUpdateCommand(
        UUID hubId,
        DeliveryManagerType type
) {
}
