package com.delivery_project.hub_service.hub.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.query.HubRouteSearchQuery;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 이동정보 목록·검색 요청 (02_hub-routes.md 8번). 모든 조건이 선택이며 주지 않은 조건은 무시된다.
 *
 * <p>페이징 파라미터({@code page}·{@code size}·{@code sort}·{@code direction})는 담지 않는다.
 * 공통 페이징 규약(00_common.md)이라 {@code PageableFactory} 가 {@code Pageable} 로 만든다.
 *
 * <p>소요 시간·거리는 각각 범위 검색이다. <b>역전({@code max < min})은 두 필드를 함께 봐야 알 수 있어
 * Bean Validation 으로 표현하지 않고 Service 가 판정한다</b> ({@code 400 INVALID_INPUT_VALUE}).
 * 값 하나로 판정되는 음수만 여기서 먼저 거른다 — 서비스 검증은 내부 호출까지 덮는 최후 방어선이라
 * 음수 규칙이 양쪽에 있는 셈이지만, 둘 다 같은 {@code 400 INVALID_INPUT_VALUE} 다.
 */
public record HubRouteSearchRequest(

		UUID departureHubId,

		UUID arrivalHubId,

		@PositiveOrZero(message = "소요 시간은 음수일 수 없습니다.")
		Integer minDurationMin,

		@PositiveOrZero(message = "소요 시간은 음수일 수 없습니다.")
		Integer maxDurationMin,

		@DecimalMin(value = "0.00", message = "거리는 음수일 수 없습니다.")
		@Digits(integer = 8, fraction = 2)
		BigDecimal minDistanceKm,

		@DecimalMin(value = "0.00", message = "거리는 음수일 수 없습니다.")
		@Digits(integer = 8, fraction = 2)
		BigDecimal maxDistanceKm
) {
	public HubRouteSearchQuery toQuery() {
		return new HubRouteSearchQuery(
				departureHubId,
				arrivalHubId,
				minDurationMin,
				maxDurationMin,
				minDistanceKm,
				maxDistanceKm
		);
	}
}
