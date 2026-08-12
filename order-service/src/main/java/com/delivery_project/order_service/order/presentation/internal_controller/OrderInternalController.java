package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.presentation.response.OrderInternalDetailResponse;
import com.delivery_project.order_service.order.presentation.response.RelatedOrderIdsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 주문 내부 API. slack-service 의 AI 파트가 FeignClient 로 호출한다.
 *
 * <p>AI 가 최종 발송 시한({@code p_ai_histories.final_dispatch_deadline})을 산출하려면
 * 무엇을 몇 개 주문했는지가 필요하다. <b>시한 자체는 AI 가 계산하는 값이라 order 가 넘기지 않는다.</b>
 *
 * <p>⚠️ 현재 호출 주체를 식별할 수단이 없다. 접근 통제는 배포 시 각 서비스 포트를 외부에
 * 노출하지 않고 게이트웨이·서비스 간 네트워크로만 접근하게 하는 것에 의존한다.
 */
@Tag(name = "주문 (내부)", description = "slack-service 연동용 주문 조회 API")
@RestController
@RequestMapping("/internal/v1/orders")
@RequiredArgsConstructor
public class OrderInternalController {

	private final OrderQueryService orderQueryService;
	private final OrderCommandService orderCommandService;

	@Operation(summary = "주문 상세 조회",
			description = "AI 발송 시한 산출에 필요한 주문 정보를 반환한다. "
					+ "공급·수령 업체, 요청사항, 주문 상품(상품 ID·수량)만 담고 상태·감사 필드는 내보내지 않는다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderInternalDetailResponse>> getOrder(
			@PathVariable UUID orderId
	) {
		OrderInternalDetailResponse response = OrderInternalDetailResponse.from(
				orderQueryService.getOrderForInternal(orderId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "업체 관련 주문 ID 조회",
			description = "해당 업체가 공급자 또는 수령자로 들어간 주문의 ID 를 모두 반환한다. "
					+ "delivery-service 가 COMPANY_MANAGER 의 배송 목록을 담당 업체 범위로 "
					+ "제한할 때 쓴다. 배송에는 업체 정보가 없고 orderId 만 있어서, 배송 건마다 "
					+ "주문을 조회하면 목록 크기만큼 호출이 나간다. 0건이면 빈 배열이다.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공 (0건이면 빈 배열)"))
	@GetMapping("/related-order-ids")
	public ResponseEntity<SuccessResponse<RelatedOrderIdsResponse>> getRelatedOrderIds(
			@RequestParam UUID companyId
	) {
		RelatedOrderIdsResponse response = RelatedOrderIdsResponse.from(
				orderQueryService.getRelatedOrderIds(companyId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "배송 완료 통보",
			description = "배송이 끝났음을 알린다. 주문이 선점하고 있던 재고를 실물 차감으로 확정하고 "
					+ "주문 이력에 ORDER_COMPLETED 를 남긴다. 주문 상태는 CONFIRMED 그대로다 — "
					+ "배송 완료는 주문 상태가 아니라 배송 상태이기 때문이다. "
					+ "같은 주문에 여러 번 호출해도 재고는 한 번만 차감된다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "완료 처리됨 · 이미 완료된 주문"),
			@ApiResponse(responseCode = "400", description = "배송이 생성되지 않은 주문"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@PostMapping("/{orderId}/complete")
	public ResponseEntity<SuccessResponse<Void>> complete(
			@PathVariable UUID orderId
	) {
		// 통보에 대한 응답이라 본문을 돌려주지 않는다. delivery 는 성공 여부만 알면 된다
		orderCommandService.complete(orderId);
		return ResponseEntity.ok(SuccessResponse.empty());
	}
}
