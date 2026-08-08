package com.delivery_project.delivery_service.delivery.application.command;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryRouteStatusUpdateCommand(
        UUID routeId,
        DeliveryRouteStatus status,
        BigDecimal actualDistanceKm,
        Integer actualDurationMin
) {
}
