package com.delivery_project.hub_service.hub.application.query;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 이동정보 목록·검색 (02_hub-routes.md 8번). 모든 조건이 선택이며 {@code null} 인 조건은 무시한다.
 *
 * <p>페이징은 여기에 담지 않는다 — {@code Pageable} 은 공통 페이징 규약(00_common.md)이 이미
 * 표준 타입으로 표현하고 있어 도메인 조건과 섞지 않는 편이 낫다.
 *
 * <p>거리는 엔티티와 같은 {@link BigDecimal} 로 받는다. 소요 시간의 {@code ...DurationMin} 처럼
 * 단위를 이름에 박아 {@code minDistanceKm}·{@code maxDistanceKm} 로 맞췄다.
 *
 * <p>범위의 음수·역전({@code max < min}) 검증은 Service 가 한다 ({@code 400 INVALID_INPUT_VALUE}).
 */
public record HubRouteSearchQuery(
		UUID departureHubId,
		UUID arrivalHubId,
		Integer minDurationMin,
		Integer maxDurationMin,
		BigDecimal minDistanceKm,
		BigDecimal maxDistanceKm
) {
}
