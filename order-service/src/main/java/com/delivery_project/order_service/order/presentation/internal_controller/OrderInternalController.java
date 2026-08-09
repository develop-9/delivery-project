package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.presentation.response.OrderInternalDetailResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
