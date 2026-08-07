package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 허브 간 경로 (02_hub-routes.md 11번 · 03_internal.md 14번). Hub Service 의 핵심 응답이다.
 *
 * <p>외부·내부 API 가 <b>같은 산출 로직</b>을 쓰고 응답에서 필드만 달리 고른다.
 * 내부는 화면 표시용인 {@code waypointHubNames} 와 최상위 허브명을 빼고 내려준다.
 */
public record HubPathResult(
		UUID departureHubId,
		String departureHubName,
		UUID arrivalHubId,
		String arrivalHubName,
		BigDecimal totalDistanceKm,
		int totalDurationMin,
		int segmentCount,
		List<String> waypointHubNames,
		List<HubPathSegmentResult> segments
) {
}
