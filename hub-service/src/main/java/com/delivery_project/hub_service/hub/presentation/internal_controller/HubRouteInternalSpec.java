package com.delivery_project.hub_service.hub.presentation.internal_controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.presentation.response.internal.HubPathInternalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 내부 배송 경로 조회 API 문서 (03_internal.md 14번). */
@Tag(name = "HubRoute (Internal)", description = "서비스 간 배송 경로 조회 API — 게이트웨이가 외부 인입을 차단한다")
public interface HubRouteInternalSpec {

	@Operation(
			summary = "배송 경로 조회 (내부)",
			description = """
					주문 생성 시 Delivery Service 가 p_delivery_routes 를 만들기 위해 호출한다.
					segments 배열이 그대로 행이 된다 — sequence, departureHubId, arrivalHubId,
					distanceKm(expected_distance_km), durationMin(expected_duration_min), hubRouteId.

					외부 경로 조회(11번)와 같은 산출 로직을 쓰되 화면 표시용 필드를 뺀 연동 전용 페이로드다.

					Delivery Service 처리 규약
					- 400·404 : 재시도 금지. 주문 생성 트랜잭션 전체 롤백
					- 401     : Feign 인터셉터의 토큰 relay 확인 후 롤백
					- 5xx·타임아웃 : 최대 3회 재시도 후 롤백
					- 상품 보관 허브 == 수령 업체 허브 : 이 API 를 호출하지 않고 경로 0건으로 처리"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "SAME_HUB_NOT_ALLOWED — Delivery 가 애초에 호출하지 않아야 하는 케이스"),
			@ApiResponse(responseCode = "404",
					description = "HUB_NOT_FOUND — 허브 없음 · HUB_ROUTE_PATH_NOT_FOUND — 구간 이동정보 없음")
	})
	ResponseEntity<SuccessResponse<HubPathInternalResponse>> findPath(UUID departureHubId, UUID arrivalHubId);
}
