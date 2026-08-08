package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryStatusUpdateRequest(

        @NotNull
        DeliveryStatus status
) {
    public DeliveryStatusUpdateCommand toCommand(
            UUID deliveryId
    ){
        return new DeliveryStatusUpdateCommand(
                deliveryId,
                status
        );
    }
}
