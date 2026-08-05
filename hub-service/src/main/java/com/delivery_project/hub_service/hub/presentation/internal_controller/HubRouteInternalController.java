package com.delivery_project.hub_service.hub.presentation.internal_controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.application.query_service.HubRouteQueryService;
import com.delivery_project.hub_service.hub.application.result.HubPathResult;
import com.delivery_project.hub_service.hub.presentation.response.HubPathInternalResponse;

import lombok.RequiredArgsConstructor;

/**
 * 내부 배송 경로 조회 API (03_internal.md 14번).
 *
 * <p>주문 생성 시 Delivery Service 가 {@code p_delivery_routes} 를 최초 1회 전체 생성하려고 호출한다.
 * 응답의 {@code segments} 배열이 그대로 행이 된다.
 *
 * <p>외부 경로 조회(11번)와 <b>같은 서비스 메서드</b>를 쓴다. 산출 로직이 갈라지면
 * 화면에 보이는 경로와 실제 배송 경로가 어긋날 수 있어서다. 응답에서 필드만 달리 고른다.
 */
@RestController
@RequestMapping("/internal/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteInternalController implements HubRouteInternalSpec {

	private final HubRouteQueryService hubRouteQueryService;

	@Override
	@GetMapping("/path")
	public ResponseEntity<SuccessResponse<HubPathInternalResponse>> findPath(
			@RequestParam UUID departureHubId,
			@RequestParam UUID arrivalHubId
	) {
		HubPathResult result = hubRouteQueryService.findPath(departureHubId, arrivalHubId);

		return ResponseEntity.ok(SuccessResponse.success(HubPathInternalResponse.from(result)));
	}
}
