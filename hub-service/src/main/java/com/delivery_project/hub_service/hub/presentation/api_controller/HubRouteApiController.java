package com.delivery_project.hub_service.hub.presentation.api_controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.hub_service.global.response.PageResponse;
import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.global.util.PageableFactory;
import com.delivery_project.hub_service.hub.application.command_service.HubRouteCommandService;
import com.delivery_project.hub_service.hub.application.query_service.HubRouteQueryService;
import com.delivery_project.hub_service.hub.application.result.HubPathResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteDetailResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteUpdateResult;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteSearchCondition;
import com.delivery_project.hub_service.hub.presentation.request.HubRouteCreateRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubRouteUpdateRequest;
import com.delivery_project.hub_service.hub.presentation.response.HubPathResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteCreateResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteDeleteResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteDetailResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteUpdateResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 외부 허브 간 이동정보 API (02_hub-routes.md).
 *
 * <p>{@code /path} 는 {@code /{hubRouteId}} 보다 구체적인 패턴이라 Spring 이 먼저 매칭한다.
 * 선언 순서와 무관하다.
 */
@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteApiController implements HubRouteApiSpec {

	private final HubRouteCommandService hubRouteCommandService;
	private final HubRouteQueryService hubRouteQueryService;

	@Override
	@PostMapping
	public ResponseEntity<SuccessResponse<HubRouteCreateResponse>> create(
			@AuthenticationPrincipal UUID callerId,
			@Valid @RequestBody HubRouteCreateRequest request
	) {
		HubRouteCreateResult result = hubRouteCommandService.create(callerId, request.toCommand());

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(SuccessResponse.success(HubRouteCreateResponse.from(result)));
	}

	/**
	 * 허브 간 경로 조회 (11번). Hub Service 의 핵심 기능이다.
	 *
	 * <p>경유지를 포함한 전체 경로가 필요할 때 쓴다. 목록 검색(8번)에 출발·도착을 함께 주면
	 * 직결 구간 한 건만 나오고 경유 구간은 나오지 않는다.
	 */
	@Override
	@GetMapping("/path")
	public ResponseEntity<SuccessResponse<HubPathResponse>> findPath(
			@RequestParam UUID departureHubId,
			@RequestParam UUID arrivalHubId
	) {
		HubPathResult result = hubRouteQueryService.findPath(departureHubId, arrivalHubId);

		return ResponseEntity.ok(SuccessResponse.success(HubPathResponse.from(result)));
	}

	@Override
	@GetMapping("/{hubRouteId}")
	public ResponseEntity<SuccessResponse<HubRouteDetailResponse>> getHubRoute(
			@PathVariable UUID hubRouteId
	) {
		HubRouteDetailResult result = hubRouteQueryService.getHubRoute(hubRouteId);

		return ResponseEntity.ok(SuccessResponse.success(HubRouteDetailResponse.from(result)));
	}

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<HubRouteDetailResponse>>> searchHubRoutes(
			@RequestParam(required = false) UUID departureHubId,
			@RequestParam(required = false) UUID arrivalHubId,
			@RequestParam(required = false) Integer minDurationMin,
			@RequestParam(required = false) Integer maxDurationMin,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction
	) {
		Pageable pageable = PageableFactory.of(page, size, sort, direction);
		HubRouteSearchCondition condition = new HubRouteSearchCondition(
				departureHubId, arrivalHubId, minDurationMin, maxDurationMin);

		Page<HubRouteDetailResult> results = hubRouteQueryService.searchHubRoutes(condition, pageable);

		return ResponseEntity.ok(
				SuccessResponse.success(PageResponse.from(results, HubRouteDetailResponse::from)));
	}

	/** 거리·시간 수정 (9번). 보낸 필드만 바뀐다. */
	@Override
	@PatchMapping("/{hubRouteId}")
	public ResponseEntity<SuccessResponse<HubRouteUpdateResponse>> update(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID hubRouteId,
			@Valid @RequestBody HubRouteUpdateRequest request
	) {
		HubRouteUpdateResult result = hubRouteCommandService.update(callerId, hubRouteId, request.toCommand());

		return ResponseEntity.ok(SuccessResponse.success(HubRouteUpdateResponse.from(result)));
	}

	@Override
	@DeleteMapping("/{hubRouteId}")
	public ResponseEntity<SuccessResponse<HubRouteDeleteResponse>> delete(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID hubRouteId
	) {
		HubRouteDeleteResult result = hubRouteCommandService.delete(callerId, hubRouteId);

		return ResponseEntity.ok(SuccessResponse.success(HubRouteDeleteResponse.from(result)));
	}
}
