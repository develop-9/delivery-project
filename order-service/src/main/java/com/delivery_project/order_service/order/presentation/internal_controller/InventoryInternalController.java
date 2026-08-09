package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.command.InventoryInternalCreateCommand;
import com.delivery_project.order_service.order.application.command_service.InventoryCommandService;
import com.delivery_project.order_service.order.presentation.request.InventoryInternalCreateRequest;
import com.delivery_project.order_service.order.presentation.response.InventoryInternalSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 내부 API. company-service 가 FeignClient 로 호출한다.
 *
 * <p>외부 API({@code /api/v1/inventories})와 경로·DTO 를 분리한다. 두 신뢰 모델
 * (사용자 role 기반 인가 vs 서비스 간 신뢰)이 한 엔드포인트에 섞이면 권한 규칙이 엉키고,
 * 내부 호출은 필요한 응답 필드도 달라서다 (팀 공통 API 규칙 제안 3).
 *
 * <p><b>재고 조회 API 는 두지 않는다.</b> 8/3 팀 회의에서 상품 서비스와 재고 서비스를 분리해
 * 재고 장애가 상품 조회까지 전파되지 않게 하기로 했다. 상품 목록·단건 조회는 재고 정보를
 * 반환하지 않으므로 company 가 재고를 물어볼 일이 없다.
 *
 * <p>⚠️ 현재 호출 주체를 식별할 수단이 없다. 접근 통제는 배포 시 각 서비스 포트를 외부에
 * 노출하지 않고 게이트웨이·서비스 간 네트워크로만 접근하게 하는 것에 의존한다.
 * 서비스 간 인증은 별도 안건이다.
 */
@Tag(name = "재고 (내부)", description = "company-service 연동용 재고 API")
@RestController
@RequestMapping("/internal/v1/inventories")
@RequiredArgsConstructor
public class InventoryInternalController {

	private final InventoryCommandService inventoryCommandService;

	@Operation(summary = "초기 재고 레코드 생성",
			description = "상품 생성 시 1회 호출한다. 보유 수량은 항상 0 으로 시작하고, 수량은 입고 API 로만 올라간다. "
					+ "409 가 나가면 호출한 쪽이 상품 생성을 롤백한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "생성 성공"),
			@ApiResponse(responseCode = "400", description = "필수값 누락"),
			@ApiResponse(responseCode = "409", description = "같은 상품·허브 재고가 이미 있음")
	})
	@PostMapping
	public ResponseEntity<SuccessResponse<InventoryInternalSummaryResponse>> create(
			@Valid @RequestBody InventoryInternalCreateRequest request
	) {
		InventoryInternalCreateCommand command = InventoryInternalCreateCommand.from(request);
		InventoryInternalSummaryResponse response = InventoryInternalSummaryResponse.from(
				inventoryCommandService.createInitial(command));
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}
}
