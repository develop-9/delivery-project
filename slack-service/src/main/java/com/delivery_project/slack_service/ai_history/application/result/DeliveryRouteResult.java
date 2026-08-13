package com.delivery_project.slack_service.ai_history.application.result;

import java.util.List;
import java.util.UUID;

public record DeliveryRouteResult(
        UUID deliveryId,
        UUID orderId,
        List<RouteResult> routes
) {

    public record RouteResult(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            Integer estimatedDurationMin
    ) {
    }
}