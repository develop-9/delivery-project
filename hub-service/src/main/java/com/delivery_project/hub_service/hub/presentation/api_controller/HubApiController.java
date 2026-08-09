package com.delivery_project.hub_service.hub.presentation.api_controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import com.delivery_project.hub_service.hub.application.command.HubDeleteCommand;
import com.delivery_project.hub_service.hub.application.command_service.HubCommandService;
import com.delivery_project.hub_service.hub.application.query.HubGetQuery;
import com.delivery_project.hub_service.hub.application.query_service.HubQueryService;
import com.delivery_project.hub_service.hub.application.result.HubCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubDetailResult;
import com.delivery_project.hub_service.hub.application.result.HubUpdateResult;
import com.delivery_project.hub_service.hub.presentation.request.HubCreateRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubSearchRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubUpdateRequest;
import com.delivery_project.hub_service.hub.presentation.response.HubCreateResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubDeleteResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubDetailResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubUpdateResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 외부 허브 API (01_hubs.md).
 *
 * <p>권한 판정은 여기가 아니라 서비스 메서드의 {@code @PreAuthorize} 가 한다.
 * 컨트롤러는 검증 → 서비스 호출 → 응답 변환만 맡는다 (팀 컨벤션 §3).
 */
@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubApiController implements HubApiSpec {

	private final HubCommandService hubCommandService;
	private final HubQueryService hubQueryService;

	@Override
	@PostMapping
	public ResponseEntity<SuccessResponse<HubCreateResponse>> create(
			@AuthenticationPrincipal UUID callerId,
			@Valid @RequestBody HubCreateRequest request
	) {
		HubCreateResult result = hubCommandService.create(callerId, request.toCommand());

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(SuccessResponse.success(HubCreateResponse.from(result)));
	}

	@Override
	@GetMapping("/{hubId}")
	public ResponseEntity<SuccessResponse<HubDetailResponse>> getHub(
			@PathVariable UUID hubId
	) {
		HubDetailResult result = hubQueryService.getHub(new HubGetQuery(hubId));

		return ResponseEntity.ok(SuccessResponse.success(HubDetailResponse.from(result)));
	}

	/**
	 * {@code size} 가 10·30·50 이 아니거나 {@code sort}·{@code direction} 이 허용값이 아니면
	 * 에러가 아니라 기본값으로 보정된다 ({@link PageableFactory}).
	 *
	 * <p>페이징 파라미터는 {@link HubSearchRequest} 에 담지 않는다 — 검색 조건이 아니라
	 * {@link PageableFactory} 가 맡는 별개 관심사다.
	 */
	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<HubDetailResponse>>> searchHubs(
			@Valid @ModelAttribute HubSearchRequest request,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction
	) {
		Pageable pageable = PageableFactory.of(page, size, sort, direction);

		Page<HubDetailResult> results = hubQueryService.searchHubs(request.toQuery(), pageable);

		return ResponseEntity.ok(
				SuccessResponse.success(PageResponse.from(results, HubDetailResponse::from)));
	}

	@Override
	@PatchMapping("/{hubId}")
	public ResponseEntity<SuccessResponse<HubUpdateResponse>> update(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID hubId,
			@Valid @RequestBody HubUpdateRequest request
	) {
		HubUpdateResult result = hubCommandService.update(callerId, request.toCommand(hubId));

		return ResponseEntity.ok(SuccessResponse.success(HubUpdateResponse.from(result)));
	}

	@Override
	@DeleteMapping("/{hubId}")
	public ResponseEntity<SuccessResponse<HubDeleteResponse>> delete(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID hubId
	) {
		HubDeleteResult result = hubCommandService.delete(callerId, new HubDeleteCommand(hubId));

		return ResponseEntity.ok(SuccessResponse.success(HubDeleteResponse.from(result)));
	}
}
