package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/** 이동정보 수정 응답 (02_hub-routes.md 9번). 생성 응답과 같은 형태다. */
public record HubRouteUpdateResult(
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
	public static HubRouteUpdateResult from(HubRoute hubRoute, String departureHubName, String arrivalHubName) {
		return new HubRouteUpdateResult(
				hubRoute.getId(),
				hubRoute.getDepartureHubId(),
				departureHubName,
				hubRoute.getArrivalHubId(),
				arrivalHubName,
				hubRoute.getDistanceKm(),
				hubRoute.getDurationMin(),
				hubRoute.getCreatedAt(),
				hubRoute.getUpdatedAt()
		);
	}
}
