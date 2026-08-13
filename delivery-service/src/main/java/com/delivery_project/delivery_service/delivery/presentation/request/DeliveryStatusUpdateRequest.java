package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.global.security.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryStatusUpdateRequest(

        @NotNull
        DeliveryStatus status
) {
    public DeliveryStatusUpdateCommand toCommand(
            UUID deliveryId,
            UUID requesterId,
            Role requesterRole
    ){
        return new DeliveryStatusUpdateCommand(
                deliveryId,
                status,
                requesterId,
                requesterRole
        );
    }
}
