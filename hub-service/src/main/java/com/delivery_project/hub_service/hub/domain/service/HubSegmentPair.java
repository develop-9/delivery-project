package com.delivery_project.hub_service.hub.domain.service;

import java.util.UUID;

/**
 * 경로 산출이 쪼갠 구간 하나 (D2). 아직 {@code p_hub_routes} 를 조회하기 전이라 거리·시간이 없다.
 */
public record HubSegmentPair(
		UUID departureHubId,
		UUID arrivalHubId
) {
}
