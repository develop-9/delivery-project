package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.global.response.PageResponse;
import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command.OrderUpdateCommand;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.facade.OrderFacade;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import com.delivery_project.order_service.order.presentation.request.OrderCreateRequest;
import com.delivery_project.order_service.order.presentation.request.OrderUpdateRequest;
import com.delivery_project.order_service.order.presentation.response.OrderResponse;
import com.delivery_project.order_service.order.presentation.response.OrderSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "주문", description = "주문 접수 · 조회 · 수정 · 취소")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderApiController {

	/** 접수·취소는 배송 연동이 얽혀 있어 Facade 를 거친다. 수정은 order 안에서 끝나 서비스를 직접 쓴다 */
	private final OrderFacade orderFacade;
	private final OrderCommandService orderCommandService;
	private final OrderQueryService orderQueryService;

	/**
	 * JWT 파싱 필터가 붙기 전까지 인증 주체가 없을 때 쓰는 팀 공통 시스템 주체 ID.
	 * 루트 .env 의 SYSTEM_ID → application.yaml 의 system.id 로 들어온다.
	 */
	@Value("${system.id}")
	private UUID systemUserId;

	@Operation(summary = "주문 접수",
			description = "상품 여러 종을 한 주문에 담는다. 수신자는 인증 주체에서 온다. "
					+ "Idempotency-Key 헤더에 요청마다 새로 만든 값(UUID 권장)을 담으면 "
					+ "같은 요청이 두 번 처리되지 않는다. 응답을 못 받아 재요청할 때 같은 키를 보내면 "
					+ "처음 만들어진 주문을 그대로 돌려받는다. 헤더가 없으면 중복 방지는 동작하지 않는다.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "접수 성공 · 같은 키의 재요청(기존 주문 반환)"),
			@ApiResponse(responseCode = "400", description = "입력값 오류 · 중복 상품"),
			@ApiResponse(responseCode = "403", description = "본인 소속이 아닌 업체로 주문"),
			@ApiResponse(responseCode = "404", description = "재고가 등록되지 않은 상품"),
			@ApiResponse(responseCode = "409", description = "같은 키의 요청이 아직 처리 중")
	})
	@PostMapping
	public ResponseEntity<SuccessResponse<OrderResponse>> create(
			@AuthenticationPrincipal JwtPrincipal principal,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody OrderCreateRequest request
	) {
		OrderCreateCommand command = OrderCreateCommand.from(receiverOrSystem(principal), request);
		OrderResponse response = OrderResponse.from(orderFacade.create(command, idempotencyKey, principal));
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 취소",
			description = "주문을 취소하고 배송도 함께 취소한다. 배송이 시작된 주문은 취소할 수 없다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "취소 성공"),
			@ApiResponse(responseCode = "400", description = "취소할 수 없는 상태"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨"),
			@ApiResponse(responseCode = "503", description = "배송 취소 실패")
	})
	@DeleteMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderResponse>> cancel(
			@AuthenticationPrincipal JwtPrincipal principal,
			@PathVariable UUID orderId
	) {
		OrderResponse response = OrderResponse.from(orderFacade.cancel(orderId, principal));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 상세 조회", description = "주문 상품 줄까지 함께 반환한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderResponse>> getOrder(
			@AuthenticationPrincipal JwtPrincipal principal,
			@PathVariable UUID orderId
	) {
		OrderResponse response = OrderResponse.from(orderQueryService.getOrder(orderId, principal));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 검색", description = "상태 · 업체 · 수신자 · 상품 · 요청사항 · 기간 조건과 페이징을 지원한다.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<OrderSummaryResponse>>> searchOrders(
			@AuthenticationPrincipal JwtPrincipal principal,
			@ModelAttribute OrderSearchCondition condition,
			Pageable pageable
	) {
		PageResponse<OrderSummaryResponse> response = PageResponse.of(
				orderQueryService.searchOrders(condition, pageable, principal), OrderSummaryResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 수정", description = "요청사항 · 상품 구성을 부분 수정한다. 배송 생성 후에는 구성 변경이 막힌다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "400", description = "변경 불가 상태 · 입력값 오류"),
			@ApiResponse(responseCode = "404", description = "주문 없음")
	})
	@PatchMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderResponse>> update(
			@AuthenticationPrincipal JwtPrincipal principal,
			@PathVariable UUID orderId,
			@Valid @RequestBody OrderUpdateRequest request
	) {
		OrderUpdateCommand command = OrderUpdateCommand.from(orderId, request);
		OrderResponse response = OrderResponse.from(orderCommandService.update(command, principal));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	/**
	 * 인증 주체가 곧 수령인이다.
	 *
	 * <p>{@code /api/v1/**} 는 인증이 걸려 있어 평소엔 principal 이 항상 있다.
	 * 그럼에도 {@code null} 을 받아주는 것은 인증을 끄고 띄우는 로컬·스모크 실행 때문이다.
	 */
	private UUID receiverOrSystem(JwtPrincipal principal) {
		return principal != null ? principal.userId() : systemUserId;
	}
}
