package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 경로를 이루는 구간 하나 (02_hub-routes.md 11번).
 *
 * <p>Delivery Service 가 이 배열을 그대로 {@code p_delivery_routes} 행으로 만든다.
 * {@code sequence} 는 1부터고 {@code hubRouteId} 는 거리·시간의 출처를 되짚기 위한 것이다.
 */
public record HubPathSegmentResult(
		int sequence,
		UUID hubRouteId,
		UUID departureHubId,
		String departureHubName,
		UUID arrivalHubId,
		String arrivalHubName,
		BigDecimal distanceKm,
		int durationMin
) {
	public static HubPathSegmentResult from(int sequence, HubRoute hubRoute, String departureHubName,
			String arrivalHubName) {

		return new HubPathSegmentResult(
				sequence,
				hubRoute.getId(),
				hubRoute.getDepartureHubId(),
				departureHubName,
				hubRoute.getArrivalHubId(),
				arrivalHubName,
				hubRoute.getDistanceKm(),
				hubRoute.getDurationMin()
		);
	}
}
