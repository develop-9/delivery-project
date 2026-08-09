package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryUpdateCommand;

import java.util.UUID;

public record DeliveryUpdateRequest(
        String deliveryAddress,
        UUID receiverUserId
) {
    public DeliveryUpdateCommand toCommand(
            UUID deliveryId
    ){
        return new DeliveryUpdateCommand(
                deliveryId,
                deliveryAddress,
                receiverUserId
        );
    }
}
