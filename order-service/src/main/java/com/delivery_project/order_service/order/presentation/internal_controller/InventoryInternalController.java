package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.command.InventoryInternalCreateCommand;
import com.delivery_project.order_service.order.application.command_service.InventoryCommandService;
import com.delivery_project.order_service.order.application.query_service.InventoryQueryService;
import com.delivery_project.order_service.order.presentation.request.InventoryInternalCreateRequest;
import com.delivery_project.order_service.order.presentation.response.InventoryInternalCreateResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryInternalDeleteResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryInternalListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
 * <p><b>상품 목록 조회에 재고를 얹지 않는다.</b> 8/3 팀 회의에서 상품 서비스와 재고 서비스를
 * 분리해 재고 장애가 상품 조회까지 전파되지 않게 하기로 했다. 그래서 여기 있는 조회는
 * <b>상품 하나</b>의 허브별 재고를 명시적으로 물어볼 때만 쓴다 — 목록 조회에서 상품마다
 * 부르면 그 결정이 무의미해진다.
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
	private final InventoryQueryService inventoryQueryService;

	/** 내부 호출에는 인증 주체가 없어 감사 필드에 쓰는 팀 공통 시스템 ID */
	@Value("${system.id}")
	private UUID systemUserId;

	@Operation(summary = "초기 재고 레코드 생성",
			description = "상품이 등록됐음을 알린다. order 가 hub-service 에서 허브 목록을 받아 "
					+ "모든 허브에 수량 0 인 재고 행을 하나씩 만든다. 이미 있는 허브는 건너뛰므로 "
					+ "같은 상품으로 다시 호출해도 안전하다. 수량은 받지 않으며 입고 API 로만 올라간다.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "생성 성공 (허브가 없으면 빈 목록)"),
			@ApiResponse(responseCode = "400", description = "입력값 오류"),
			@ApiResponse(responseCode = "503", description = "허브 목록을 가져올 수 없음")
	})
	@PostMapping
	public ResponseEntity<SuccessResponse<InventoryInternalCreateResponse>> createInitial(
			@Valid @RequestBody InventoryInternalCreateRequest request
	) {
		InventoryInternalCreateCommand command = InventoryInternalCreateCommand.from(request);
		InventoryInternalCreateResponse response = InventoryInternalCreateResponse.from(
				inventoryCommandService.createInitial(command));
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@Operation(summary = "상품별 허브 재고 조회",
			description = "상품 하나가 각 허브에 얼마나 있는지 돌려준다. 상품 하나의 행 수는 허브 수로 "
					+ "고정돼 있어 페이징하지 않는다. 재고가 없어도 오류가 아니라 빈 목록이다. "
					+ "선점 수량은 order 내부 사정이라 담지 않고 주문 가능한 수량만 준다.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 목록)"))
	@GetMapping
	public ResponseEntity<SuccessResponse<InventoryInternalListResponse>> getByProduct(
			@RequestParam UUID productId
	) {
		InventoryInternalListResponse response = InventoryInternalListResponse.from(
				inventoryQueryService.getInventoriesByProduct(productId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "상품 재고 일괄 삭제",
			description = "상품이 삭제됐음을 알린다. 그 상품의 재고를 허브 구분 없이 모두 논리 삭제한다. "
					+ "선점된 재고가 있으면 400 으로 막는다 — 진행 중인 주문이 잡고 있는 물량을 지우면 "
					+ "배송 완료 시 차감할 대상이 사라진다. 지울 재고가 없어도 오류가 아니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공 (지울 것이 없으면 빈 목록)"),
			@ApiResponse(responseCode = "400", description = "선점된 재고가 있어 삭제할 수 없음")
	})
	@DeleteMapping("/{productId}")
	public ResponseEntity<SuccessResponse<InventoryInternalDeleteResponse>> deleteByProduct(
			@PathVariable UUID productId
	) {
		// 내부 호출에는 사용자 토큰이 없어 감사 주체를 팀 공통 시스템 ID 로 남긴다
		InventoryInternalDeleteResponse response = InventoryInternalDeleteResponse.from(
				inventoryCommandService.deleteByProduct(productId, systemUserId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
