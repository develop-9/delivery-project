package com.delivery_project.hub_service.hub.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 이동정보 목록·검색 조건 (문서 8번). 모든 필드가 {@code null} 가능하며 {@code null} 인 조건은 무시한다.
 *
 * <p>{@code departureHubId} 와 {@code arrivalHubId} 를 둘 다 주면 유향 특성상 결과는 최대 1건이다.
 * 경유지를 포함한 전체 경로가 필요하면 이 검색이 아니라 경로 조회(문서 11번)를 쓴다.
 *
 * <p>소요 시간·거리는 각각 범위 검색이다. <b>음수와 역전({@code max < min}) 검증은 Service 가 한다</b>
 * ({@code 400 INVALID_INPUT_VALUE}). 리포지토리는 걸러진 값이 온다고 보고 조건만 조립한다.
 *
 * <p>거리는 엔티티 {@code HubRoute.distanceKm} 와 같은 {@link BigDecimal} 이다.
 */
public record HubRouteSearchCondition(
		UUID departureHubId,
		UUID arrivalHubId,
		Integer minDurationMin,
		Integer maxDurationMin,
		BigDecimal minDistanceKm,
		BigDecimal maxDistanceKm
) {
}
