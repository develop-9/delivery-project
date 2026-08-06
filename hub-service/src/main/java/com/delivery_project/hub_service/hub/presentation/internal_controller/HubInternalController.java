package com.delivery_project.hub_service.hub.presentation.internal_controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.application.query.HubBatchQuery;
import com.delivery_project.hub_service.hub.application.query.HubSummaryQuery;
import com.delivery_project.hub_service.hub.application.query_service.HubQueryService;
import com.delivery_project.hub_service.hub.application.result.HubBatchResult;
import com.delivery_project.hub_service.hub.application.result.HubSummaryResult;
import com.delivery_project.hub_service.hub.presentation.response.internal.HubBatchResponse;
import com.delivery_project.hub_service.hub.presentation.response.internal.HubSummaryResponse;

import lombok.RequiredArgsConstructor;

/**
 * 내부 허브 조회 API (03_internal.md 12·13번). 타 서비스가 FeignClient 로 호출한다.
 *
 * <p>⚠️ {@code X-} 커스텀 헤더를 쓰지 않아 <b>내부·외부 호출을 구분할 수 없다.</b>
 * 접근 통제는 전적으로 게이트웨이 라우팅이 외부 인입을 차단하는 데 의존하며,
 * 라우팅이 잘못 열려도 이 서비스는 탐지하지 못하고 정상 응답한다.
 *
 * <p>그래서 내부 API 는 {@code 403 AUTH_FORBIDDEN} 을 내지 않는다 — 호출 주체를 식별할 수 없어
 * "내부 경로 진입 규칙 위반"이라는 판정 자체가 불가능하다.
 */
@RestController
@RequestMapping("/internal/v1/hubs")
@RequiredArgsConstructor
public class HubInternalController implements HubInternalSpec {

	private final HubQueryService hubQueryService;

	@Override
	@GetMapping("/{hubId}")
	public ResponseEntity<SuccessResponse<HubSummaryResponse>> getHub(
			@PathVariable UUID hubId
	) {
		HubSummaryResult result = hubQueryService.getHubSummary(new HubSummaryQuery(hubId));

		return ResponseEntity.ok(SuccessResponse.success(HubSummaryResponse.from(result)));
	}

	/**
	 * 다건 조회 (13번). N+1 Feign 호출을 막기 위한 배치다.
	 *
	 * <p>{@code hubIds} 는 콤마 구분이고 Spring 이 {@code List<UUID>} 로 풀어준다.
	 */
	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<HubBatchResponse>> getHubs(
			@RequestParam List<UUID> hubIds
	) {
		HubBatchResult result = hubQueryService.getHubs(new HubBatchQuery(hubIds));

		return ResponseEntity.ok(SuccessResponse.success(HubBatchResponse.from(result)));
	}
}
