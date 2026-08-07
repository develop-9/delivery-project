package com.delivery_project.hub_service.hub.presentation.response.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubPathResult;
import com.delivery_project.hub_service.hub.presentation.response.HubPathSegmentResponse;

/**
 * 내부 배송 경로 조회 응답 (03_internal.md 14번).
 *
 * <p>외부 조회와 <b>산출 로직이 같고</b> 화면 표시용 필드만 뺀 연동 전용 페이로드다 —
 * 최상위 허브명과 {@code waypointHubNames} 가 없다.
 *
 * <p>Delivery Service 가 {@code segments} 를 그대로 {@code p_delivery_routes} 행으로 만든다.
 */
public record HubPathInternalResponse(
		UUID departureHubId,
		UUID arrivalHubId,
		BigDecimal totalDistanceKm,
		int totalDurationMin,
		int segmentCount,
		List<HubPathSegmentResponse> segments
) {
	public static HubPathInternalResponse from(HubPathResult result) {
		return new HubPathInternalResponse(
				result.departureHubId(),
				result.arrivalHubId(),
				result.totalDistanceKm(),
				result.totalDurationMin(),
				result.segmentCount(),
				result.segments().stream().map(HubPathSegmentResponse::from).toList()
		);
	}
}
