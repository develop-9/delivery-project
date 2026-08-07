package com.delivery_project.hub_service.hub.presentation.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubPathSegmentResult;

/** 경로 구간 하나. 외부·내부 경로 조회가 같은 형태를 쓴다. */
public record HubPathSegmentResponse(
		int sequence,
		UUID hubRouteId,
		UUID departureHubId,
		String departureHubName,
		UUID arrivalHubId,
		String arrivalHubName,
		BigDecimal distanceKm,
		int durationMin
) {
	public static HubPathSegmentResponse from(HubPathSegmentResult result) {
		return new HubPathSegmentResponse(
				result.sequence(),
				result.hubRouteId(),
				result.departureHubId(),
				result.departureHubName(),
				result.arrivalHubId(),
				result.arrivalHubName(),
				result.distanceKm(),
				result.durationMin()
		);
	}
}
