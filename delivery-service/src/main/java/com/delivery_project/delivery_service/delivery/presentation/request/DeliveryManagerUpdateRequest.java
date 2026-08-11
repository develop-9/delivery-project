package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerUpdateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.global.security.Role;

import java.util.UUID;

public record DeliveryManagerUpdateRequest(
        UUID hubId,
        DeliveryManagerType type
) {
    public DeliveryManagerUpdateCommand toCommand(
            UUID managerId,
            UUID requesterId,
            Role requesterRole
    ){
        return new DeliveryManagerUpdateCommand(
                managerId,
                hubId,
                type,
                requesterId,
                requesterRole
        );
    }
}
