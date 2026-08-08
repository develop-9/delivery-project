package com.delivery_project.hub_service.hub.presentation.internal_controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.delivery_project.hub_service.global.response.SuccessResponse;
import com.delivery_project.hub_service.hub.presentation.response.internal.HubBatchResponse;
import com.delivery_project.hub_service.hub.presentation.response.internal.HubSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 내부 허브 조회 API 문서 (03_internal.md). */
@Tag(name = "Hub (Internal)", description = "서비스 간 허브 조회 API — 게이트웨이가 외부 인입을 차단한다")
public interface HubInternalSpec {

	@Operation(
			summary = "허브 단건 조회 (내부)",
			description = """
					허브 존재 여부와 기본 정보를 검증할 때 쓴다.
					User(회원가입 승인 시 소속 허브), Company(업체·상품의 관리 허브), Delivery(배송 담당자 소속 허브)가 호출한다.

					404 면 호출 서비스는 자기 트랜잭션을 롤백하고 HUB_NOT_FOUND 사유를 전파한다. 재시도는 금지다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 없거나 논리 삭제됨")
	})
	ResponseEntity<SuccessResponse<HubSummaryResponse>> getHub(UUID hubId);

	@Operation(
			summary = "허브 다건 조회 (내부)",
			description = """
					N+1 Feign 호출을 막기 위한 배치 조회. Order(AI 프롬프트의 허브명), Delivery(경로 목록의 허브명),
					Slack(알림 본문의 허브명 치환)이 호출한다.

					hubIds 는 콤마로 구분하며 1~17개까지 받는다.
					요청 순서와 응답 순서는 보장하지 않으므로 호출 측이 hubId 를 키로 매핑해서 쓴다.

					일부만 없는 것은 에러가 아니라 200 + notFoundHubIds 로 내려간다.
					하나도 못 찾았을 때만 404 다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공 (부분 실패 포함)"),
			@ApiResponse(responseCode = "400", description = "INVALID_INPUT_VALUE — hubIds 누락·빈 값·17개 초과"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND — 요청한 허브가 하나도 없음")
	})
	ResponseEntity<SuccessResponse<HubBatchResponse>> getHubs(List<UUID> hubIds);
}
