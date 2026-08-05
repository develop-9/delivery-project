package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.global.response.PageResponse;
import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import com.delivery_project.order_service.order.presentation.request.OrderCancelRequest;
import com.delivery_project.order_service.order.presentation.request.OrderCreateRequest;
import com.delivery_project.order_service.order.presentation.request.OrderUpdateRequest;
import com.delivery_project.order_service.order.presentation.response.OrderResponse;
import com.delivery_project.order_service.order.presentation.response.OrderSnapshotResponse;
import com.delivery_project.order_service.order.presentation.response.OrderSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "주문", description = "주문 접수 · 조회 · 수정 · 취소 · 이력")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderApiController {

	private final OrderCommandService orderCommandService;
	private final OrderQueryService orderQueryService;

	@Operation(summary = "주문 접수", description = "상품 여러 종을 한 주문에 담는다. 금액은 서버가 계산한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "접수 성공"),
			@ApiResponse(responseCode = "400", description = "입력값 오류 · 중복 상품")
	})
	@PostMapping
	public ResponseEntity<SuccessResponse<OrderResponse>> create(
			@Valid @RequestBody OrderCreateRequest request
	) {
		OrderResponse response = OrderResponse.from(orderCommandService.create(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 상세 조회", description = "주문 상품 줄까지 함께 반환한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderResponse>> getOrder(
			@PathVariable UUID orderId
	) {
		OrderResponse response = OrderResponse.from(orderQueryService.getOrder(orderId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 검색", description = "상태 · 업체 · 허브 · 상품 · 키워드 · 기간 조건과 페이징을 지원한다.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<OrderSummaryResponse>>> searchOrders(
			@ModelAttribute OrderSearchCondition condition,
			Pageable pageable
	) {
		PageResponse<OrderSummaryResponse> response = PageResponse.of(
				orderQueryService.searchOrders(condition, pageable), OrderSummaryResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 이력 타임라인", description = "사건별 스냅샷을 반환한다. eventType 으로 걸러 볼 수 있다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping("/{orderId}/snapshots")
	public ResponseEntity<SuccessResponse<PageResponse<OrderSnapshotResponse>>> getSnapshots(
			@PathVariable UUID orderId,
			@RequestParam(required = false) EventType eventType,
			Pageable pageable
	) {
		PageResponse<OrderSnapshotResponse> response = PageResponse.of(
				orderQueryService.getSnapshots(orderId, eventType, pageable), OrderSnapshotResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 이력 단건 조회", description = "해당 주문에 속한 이력만 조회된다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "이력 없음 · 다른 주문의 이력")
	})
	@GetMapping("/{orderId}/snapshots/{snapshotId}")
	public ResponseEntity<SuccessResponse<OrderSnapshotResponse>> getSnapshot(
			@PathVariable UUID orderId,
			@PathVariable UUID snapshotId
	) {
		OrderSnapshotResponse response = OrderSnapshotResponse.from(
				orderQueryService.getSnapshot(orderId, snapshotId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 수정", description = "요청사항 · 납품 기한 · 상품 구성을 부분 수정한다. 배송 생성 후에는 구성 변경이 막힌다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "400", description = "변경 불가 상태 · 입력값 오류"),
			@ApiResponse(responseCode = "404", description = "주문 없음")
	})
	@PatchMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<OrderResponse>> update(
			@PathVariable UUID orderId,
			@Valid @RequestBody OrderUpdateRequest request
	) {
		OrderResponse response = OrderResponse.from(orderCommandService.update(request.toCommand(orderId)));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 취소", description = "취소 사유는 필수다. 종료 상태의 주문은 다시 취소할 수 없다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "취소 성공"),
			@ApiResponse(responseCode = "400", description = "허용되지 않는 상태 전이"),
			@ApiResponse(responseCode = "404", description = "주문 없음")
	})
	@PatchMapping("/{orderId}/cancel")
	public ResponseEntity<SuccessResponse<OrderResponse>> cancel(
			@PathVariable UUID orderId,
			@Valid @RequestBody OrderCancelRequest request
	) {
		OrderResponse response = OrderResponse.from(orderCommandService.cancel(request.toCommand(orderId)));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 삭제", description = "논리 삭제. 진행 중인 주문은 취소를 먼저 거쳐야 한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "400", description = "진행 중인 주문"),
			@ApiResponse(responseCode = "404", description = "주문 없음")
	})
	@DeleteMapping("/{orderId}")
	public ResponseEntity<SuccessResponse<Void>> delete(
			@PathVariable UUID orderId
	) {
		orderCommandService.delete(orderId);
		return ResponseEntity.ok(SuccessResponse.empty());
	}
}
