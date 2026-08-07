package com.delivery_project.hub_service.hub.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubRouteCreateResult;

public record HubRouteCreateResponse(
		UUID hubRouteId,
		UUID departureHubId,
		String departureHubName,
		UUID arrivalHubId,
		String arrivalHubName,
		BigDecimal distanceKm,
		int durationMin,
		Instant createdAt,
		Instant updatedAt
) {
	public static HubRouteCreateResponse from(HubRouteCreateResult result) {
		return new HubRouteCreateResponse(
				result.hubRouteId(),
				result.departureHubId(),
				result.departureHubName(),
				result.arrivalHubId(),
				result.arrivalHubName(),
				result.distanceKm(),
				result.durationMin(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
