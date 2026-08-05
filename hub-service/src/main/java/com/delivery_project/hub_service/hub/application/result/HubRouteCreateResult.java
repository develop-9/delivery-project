package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 이동정보 생성 응답 (02_hub-routes.md 6번).
 *
 * <p>문서에 있는 {@code distanceSource} 는 담지 않는다. 거리·시간의 출처가 지도 API 인지
 * 수동 입력인지 알려주는 필드인데, 지금은 수동 입력뿐이라 값이 하나로 고정된다.
 * 지도 API 연동이 들어오면 되살린다.
 */
public record HubRouteCreateResult(
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
	public static HubRouteCreateResult from(HubRoute hubRoute, String departureHubName, String arrivalHubName) {
		return new HubRouteCreateResult(
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
