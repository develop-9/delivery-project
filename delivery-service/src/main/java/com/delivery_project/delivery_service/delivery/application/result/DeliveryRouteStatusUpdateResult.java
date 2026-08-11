package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryRouteStatusUpdateResult(
        UUID routeId,
        DeliveryRouteStatus previousStatus,
        DeliveryRouteStatus currentStatus,
        Instant updatedAt
) {
}
