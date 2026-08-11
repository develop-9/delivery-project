package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerCreateCommand(
        UUID userId,
        UUID hubId,
        DeliveryManagerType type,
        UUID requesterId,
        Role requesterRole
) {
}
