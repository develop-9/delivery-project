package com.delivery_project.hub_service.hub.presentation.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubPathResult;

/**
 * 외부 경로 조회 응답 (02_hub-routes.md 11번).
 *
 * <p>{@code waypointHubNames} 는 출발·도착을 뺀 경유 허브명이고 화면과 AI 프롬프트의 "경유지"에 쓴다.
 * 연동 전용인 내부 API 는 이 필드를 담지 않는다
 * ({@link com.delivery_project.hub_service.hub.presentation.response.internal.HubPathInternalResponse}).
 */
public record HubPathResponse(
		UUID departureHubId,
		String departureHubName,
		UUID arrivalHubId,
		String arrivalHubName,
		BigDecimal totalDistanceKm,
		int totalDurationMin,
		int segmentCount,
		List<String> waypointHubNames,
		List<HubPathSegmentResponse> segments
) {
	public static HubPathResponse from(HubPathResult result) {
		return new HubPathResponse(
				result.departureHubId(),
				result.departureHubName(),
				result.arrivalHubId(),
				result.arrivalHubName(),
				result.totalDistanceKm(),
				result.totalDurationMin(),
				result.segmentCount(),
				result.waypointHubNames(),
				result.segments().stream().map(HubPathSegmentResponse::from).toList()
		);
	}
}
