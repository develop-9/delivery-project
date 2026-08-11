package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryStatusUpdateCommand(
        UUID deliveryId,
        DeliveryStatus status,
        UUID requesterId,
        Role requesterRole
) {

}
