package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HubRoutePathResponse(
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal totalDistanceKm,
        Integer totalDurationMin,
        Integer segmentCount,
        List<HubRouteSegmentResponse> segments
) {
}