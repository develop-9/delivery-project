package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerUpdateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.util.UUID;

public record DeliveryManagerUpdateRequest(
        UUID hubId,
        DeliveryManagerType type
) {
    public DeliveryManagerUpdateCommand toCommand(
            UUID managerId
    ){
        return new DeliveryManagerUpdateCommand(
                managerId, hubId, type
        );
    }
}
