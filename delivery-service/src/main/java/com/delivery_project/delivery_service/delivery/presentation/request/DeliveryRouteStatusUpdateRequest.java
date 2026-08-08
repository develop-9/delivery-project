package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryRouteStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.global.security.Role;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryRouteStatusUpdateRequest(

        @NotNull
        DeliveryRouteStatus status,

        BigDecimal actualDistanceKm,
        Integer actualDurationMin
) {
    public DeliveryRouteStatusUpdateCommand toCommand(
            UUID routeId,
            UUID requesterId,
            Role requesterRole
    ){
        return new DeliveryRouteStatusUpdateCommand(
                routeId,
                status,
                actualDistanceKm,
                actualDurationMin,
                requesterId,
                requesterRole
        );
    }
}
