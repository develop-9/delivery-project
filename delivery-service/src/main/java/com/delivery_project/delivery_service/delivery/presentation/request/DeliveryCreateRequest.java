package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryCreateRequest(
        @NotNull
        UUID orderId,

        @NotNull
        UUID departureHubId,

        @NotNull
        UUID destinationHubId,

        @NotBlank
        String deliveryAddress,

        @NotNull
        UUID receiverUserId
) {
    public DeliveryCreateCommand toCommand() {
        return new DeliveryCreateCommand(
                orderId,
                departureHubId,
                destinationHubId,
                deliveryAddress,
                receiverUserId
        );
    }
}
