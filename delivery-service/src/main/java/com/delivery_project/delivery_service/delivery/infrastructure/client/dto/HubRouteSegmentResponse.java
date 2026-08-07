package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HubRouteSegmentResponse(
        Integer sequence,
        UUID hubRouteId,
        UUID departureHubId,
        String departureHubName,
        UUID arrivalHubId,
        String arrivalHubName,
        BigDecimal distanceKm,
        Integer durationMin
) {
}