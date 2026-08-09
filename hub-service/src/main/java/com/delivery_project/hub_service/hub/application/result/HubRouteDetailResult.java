package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 이동정보 단건·목록 조회 응답 (02_hub-routes.md 7·8번).
 *
 * <p>허브와 달리 감사 필드를 담는다 — 이동정보는 거리·시간이 나중에 갱신될 수 있어
 * {@code updatedAt} 이 "이 값이 언제 기준인지"를 알려주는 실질 정보이기 때문이다.
 */
public record HubRouteDetailResult(
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
	public static HubRouteDetailResult from(HubRoute hubRoute, String departureHubName,
			String arrivalHubName) {

		return new HubRouteDetailResult(
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
