package com.delivery_project.hub_service.hub.presentation.api_controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.delivery_project.hub_service.global.response.PageResponse;
import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.presentation.request.HubCreateRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubUpdateRequest;
import com.delivery_project.hub_service.hub.presentation.response.HubCreateResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubDeleteResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubDetailResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubUpdateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 허브 API 문서. 컨벤션 §12 대로 Swagger 애너테이션을 컨트롤러에서 분리했다.
 *
 * <p>전 API 공통 에러라 개별 메서드에 적지 않은 것 — {@code 401 AUTH_UNAUTHORIZED}(토큰 없음·만료·서명 불일치),
 * {@code 400 INVALID_INPUT_VALUE}(UUID 형식 오류·Validation 실패).
 */
@Tag(name = "Hub", description = "허브 관리 API")
public interface HubApiSpec {

	@Operation(
			summary = "허브 생성",
			description = """
					새 허브를 등록한다. MASTER 전용.

					hubType 이 MAIN 이면 parentHubId 를 보내지 않는다 — 서버가 발급한 자기 ID 로 채운다 (D1).
					hubType 이 SUB 면 parentHubId 가 필수이고, 그 허브는 MAIN 이어야 한다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "생성 성공"),
			@ApiResponse(responseCode = "400",
					description = "PARENT_HUB_REQUIRED · PARENT_HUB_NOT_ALLOWED · PARENT_HUB_NOT_MAIN"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "PARENT_HUB_NOT_FOUND"),
			@ApiResponse(responseCode = "409", description = "DUPLICATE_HUB_NAME")
	})
	ResponseEntity<SuccessResponse<HubCreateResponse>> create(
			@Parameter(hidden = true) UUID callerId,
			HubCreateRequest request
	);

	@Operation(
			summary = "허브 단건 조회",
			description = "허브 하나를 조회한다. 로그인한 사용자면 권한 제한 없이 읽을 수 있다. 감사 필드는 응답에 담지 않는다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 없거나 논리 삭제됨")
	})
	ResponseEntity<SuccessResponse<HubDetailResponse>> getHub(UUID hubId);

	@Operation(
			summary = "허브 목록 조회 · 검색",
			description = """
					조건에 맞는 허브를 페이징해서 돌려준다. 결과 0건도 200 이다.

					parentHubId 는 D1 자기참조 때문에 해당 중앙 허브 자신도 포함한다.
					자신을 빼려면 hubType=SUB 를 함께 준다.

					size 는 10·30·50 만 허용하며 그 외 값은 에러가 아니라 10 으로 보정된다.
					sort 는 createdAt·updatedAt, direction 은 asc·desc 만 허용하고 나머지는 기본값으로 되돌린다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — parentHubId 허브 없음")
	})
	ResponseEntity<SuccessResponse<PageResponse<HubDetailResponse>>> searchHubs(
			String keyword,
			HubType hubType,
			UUID parentHubId,
			Integer page,
			Integer size,
			String sort,
			String direction
	);

	@Operation(
			summary = "허브 수정",
			description = """
					전달한 필드만 수정한다. MASTER 전용.

					hubType 전환 규칙
					- SUB → MAIN : parentHubId 를 서버가 자기 자신으로 덮어쓴다
					- MAIN → SUB : parentHubId 필수. 자기 제외 하위 허브가 0건이어야 한다 (D7)
					- SUB → SUB  : parentHubId 를 보내면 교체, 대상은 MAIN 이어야 한다
					- MAIN → MAIN: parentHubId 를 담으면 400"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "400",
					description = "PARENT_HUB_REQUIRED · PARENT_HUB_NOT_ALLOWED · PARENT_HUB_NOT_MAIN"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND · PARENT_HUB_NOT_FOUND"),
			@ApiResponse(responseCode = "409", description = "DUPLICATE_HUB_NAME · HUB_HAS_CHILDREN")
	})
	ResponseEntity<SuccessResponse<HubUpdateResponse>> update(
			@Parameter(hidden = true) UUID callerId,
			UUID hubId,
			HubUpdateRequest request
	);

	@Operation(
			summary = "허브 삭제",
			description = """
					논리 삭제. MASTER 전용.

					이 허브가 출발지 또는 도착지인 이동정보도 같은 트랜잭션에서 함께 논리 삭제된다 (D6).
					하나라도 실패하면 전체 롤백이라 허브만 사라지고 구간이 남는 중간 상태는 없다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 없거나 이미 논리 삭제됨"),
			@ApiResponse(responseCode = "409", description = "HUB_HAS_CHILDREN — 자기 제외 관할 허브가 남아 있음")
	})
	ResponseEntity<SuccessResponse<HubDeleteResponse>> delete(
			@Parameter(hidden = true) UUID callerId,
			UUID hubId
	);
}
