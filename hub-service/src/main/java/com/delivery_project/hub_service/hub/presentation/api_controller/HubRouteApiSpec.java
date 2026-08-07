package com.delivery_project.hub_service.hub.presentation.api_controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import org.springdoc.core.annotations.ParameterObject;

import com.delivery_project.hub_service.global.response.PageResponse;
import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.presentation.request.HubRouteCreateRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubRouteSearchRequest;
import com.delivery_project.hub_service.hub.presentation.request.HubRouteUpdateRequest;
import com.delivery_project.hub_service.hub.presentation.response.HubPathResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteCreateResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteDeleteResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteDetailResponse;
import com.delivery_project.hub_service.hub.presentation.response.HubRouteUpdateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 허브 간 이동정보 API 문서. 컨벤션 §12 대로 Swagger 애너테이션을 컨트롤러에서 분리했다.
 *
 * <p>전 API 공통이라 개별 메서드에 적지 않은 것 — {@code 401 AUTH_UNAUTHORIZED},
 * {@code 400 INVALID_INPUT_VALUE}.
 */
@Tag(name = "HubRoute", description = "허브 간 이동정보 API")
public interface HubRouteApiSpec {

	@Operation(
			summary = "이동정보 생성",
			description = """
					구간 하나(출발 → 도착)를 등록한다. MASTER 전용.

					경로는 유향이라 A→B 가 있어도 B→A 등록은 중복이 아니다 (D3).

					거리·시간은 요청이 준 값을 그대로 저장하며 둘 다 필수다.
					문서(D8)의 지도 API 자동 산출은 아직 연동하지 않았다.

					구간은 Hub-and-Spoke 규칙을 지켜야 한다 — SUB→자기 관할 MAIN, MAIN→자기 관할 SUB,
					MAIN↔MAIN 만 허용된다. 타입 조합이 맞아도 관할 관계가 아니면 거부한다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "생성 성공"),
			@ApiResponse(responseCode = "400", description = "SAME_HUB_NOT_ALLOWED · INVALID_HUB_ROUTE_SEGMENT"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 출발·도착 허브 없음"),
			@ApiResponse(responseCode = "409", description = "DUPLICATE_HUB_ROUTE")
	})
	ResponseEntity<SuccessResponse<HubRouteCreateResponse>> create(
			@Parameter(hidden = true) UUID callerId,
			HubRouteCreateRequest request
	);

	@Operation(
			summary = "허브 간 경로 조회",
			description = """
					출발 허브에서 도착 허브까지의 전체 경로를 구간 단위로 돌려준다. Hub Service 의 핵심 기능이다.

					그래프 탐색이 아니라 parentHubId 계층 규칙으로 산출한다 (D2).
					구간 수는 1~3 이며 서로 다른 MAIN 관할의 SUB → SUB 가 최대 케이스다.

					경유지를 포함한 전체 경로가 필요할 때 쓴다. 목록 검색에 출발·도착을 함께 주면
					직결 구간 한 건만 나오고 경유 구간은 나오지 않는다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "SAME_HUB_NOT_ALLOWED"),
			@ApiResponse(responseCode = "404",
					description = "HUB_NOT_FOUND — 허브 없음 · HUB_ROUTE_PATH_NOT_FOUND — 구간 이동정보 없음")
	})
	ResponseEntity<SuccessResponse<HubPathResponse>> findPath(UUID departureHubId, UUID arrivalHubId);

	@Operation(summary = "이동정보 단건 조회", description = "구간 하나를 조회한다. 로그인한 사용자면 권한 제한 없이 읽을 수 있다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404",
					description = "HUB_ROUTE_NOT_FOUND — 없거나 논리 삭제됨 (허브 삭제로 연쇄 삭제된 경우 포함)")
	})
	ResponseEntity<SuccessResponse<HubRouteDetailResponse>> getHubRoute(UUID hubRouteId);

	@Operation(
			summary = "이동정보 목록 조회 · 검색",
			description = """
					조건에 맞는 구간을 페이징해서 돌려준다. 결과 0건도 200 이다.

					departureHubId 와 arrivalHubId 를 둘 다 주면 유향 특성상 결과는 최대 1건이다.

					소요 시간(minDurationMin·maxDurationMin)과 거리(minDistanceKm·maxDistanceKm)는
					각각 범위 검색이다. 한쪽만 주면 열린 범위로 본다.

					size 는 10·30·50 만 허용하며 그 외 값은 에러가 아니라 10 으로 보정된다.
					sort 는 createdAt·updatedAt 에 더해 durationMin·distanceKm 를 허용한다 —
					허용 밖의 값도 에러가 아니라 createdAt 으로 보정된다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400",
					description = "INVALID_INPUT_VALUE — maxDurationMin < minDurationMin, 소요 시간 음수, "
							+ "maxDistanceKm < minDistanceKm, 거리 음수"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 출발·도착 허브 없음")
	})
	ResponseEntity<SuccessResponse<PageResponse<HubRouteDetailResponse>>> searchHubRoutes(
			@ParameterObject HubRouteSearchRequest request,
			Integer page,
			Integer size,
			String sort,
			String direction
	);

	@Operation(
			summary = "이동정보 수정",
			description = """
					거리·시간만 수정한다. MASTER 전용.
					출발·도착 허브는 구간의 정체성이라 바꿀 수 없다 — 구간을 옮기려면 삭제 후 재등록한다.

					보낸 필드만 바뀌고 생략한 필드는 현재 값을 유지한다.
					빈 바디({})는 아무것도 바꾸지 않지만 에러도 아니다 —
					지도 API 연동이 들어오면 그 형태가 재계산 요청이 될 자리다.

					departureHubId·arrivalHubId 를 함께 보내도 에러가 아니라 무시된다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "HUB_ROUTE_NOT_FOUND")
	})
	ResponseEntity<SuccessResponse<HubRouteUpdateResponse>> update(
			@Parameter(hidden = true) UUID callerId,
			UUID hubRouteId,
			HubRouteUpdateRequest request
	);

	@Operation(
			summary = "이동정보 삭제",
			description = """
					논리 삭제. MASTER 전용.

					이 구간이 필요한 경로가 있어도 막지 않는다. 영향 범위(affectedHubPairCount)만
					응답에 담아주고 판단은 호출자에게 맡긴다.

					삭제 후 이 구간을 포함하는 경로 조회는 404 HUB_ROUTE_PATH_NOT_FOUND 로 실패하고,
					결과적으로 그 구간을 지나는 주문 생성이 막힌다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "403", description = "AUTH_FORBIDDEN — MASTER 가 아님"),
			@ApiResponse(responseCode = "404", description = "HUB_ROUTE_NOT_FOUND — 없거나 이미 논리 삭제됨")
	})
	ResponseEntity<SuccessResponse<HubRouteDeleteResponse>> delete(
			@Parameter(hidden = true) UUID callerId,
			UUID hubRouteId
	);
}
