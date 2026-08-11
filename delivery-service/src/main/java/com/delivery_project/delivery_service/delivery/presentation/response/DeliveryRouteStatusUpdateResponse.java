package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryRouteStatusUpdateResponse(
        UUID routeId,
        DeliveryRouteStatus previousStatus,
        DeliveryRouteStatus currentStatus,
        Instant updatedAt
) {
    public static DeliveryRouteStatusUpdateResponse from(
            DeliveryRouteStatusUpdateResult result
    ) {
        return new DeliveryRouteStatusUpdateResponse(
                result.routeId(),
                result.previousStatus(),
                result.currentStatus(),
                result.updatedAt()
        );
    }
}
