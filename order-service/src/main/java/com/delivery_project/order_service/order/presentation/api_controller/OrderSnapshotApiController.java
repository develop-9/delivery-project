package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.global.response.PageResponse;
import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.query_service.OrderSnapshotQueryService;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.presentation.response.OrderSnapshotResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 주문 이력(스냅샷) 조회.
 *
 * 주문과 이력은 관리하는 데이터와 응답 책임이 달라 컨트롤러를 분리한다.
 * 이력은 사용자가 만들거나 고치는 대상이 아니라 주문 변경 시 서버가 남기는 기록이므로 조회만 제공한다.
 */
@Tag(name = "주문 이력", description = "주문 이력(스냅샷) 조회")
@RestController
@RequestMapping("/api/v1/orders/{orderId}/snapshots")
@RequiredArgsConstructor
public class OrderSnapshotApiController {

	private final OrderSnapshotQueryService orderSnapshotQueryService;

	@Operation(summary = "주문 이력 타임라인", description = "사건별 스냅샷을 반환한다. eventType 으로 걸러 볼 수 있다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<OrderSnapshotResponse>>> getSnapshots(
			@PathVariable UUID orderId,
			@RequestParam(required = false) EventType eventType,
			Pageable pageable
	) {
		PageResponse<OrderSnapshotResponse> response = PageResponse.of(
				orderSnapshotQueryService.getSnapshots(orderId, eventType, pageable), OrderSnapshotResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 이력 단건 조회", description = "해당 주문에 속한 이력만 조회된다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "이력 없음 · 다른 주문의 이력")
	})
	@GetMapping("/{snapshotId}")
	public ResponseEntity<SuccessResponse<OrderSnapshotResponse>> getSnapshot(
			@PathVariable UUID orderId,
			@PathVariable UUID snapshotId
	) {
		OrderSnapshotResponse response = OrderSnapshotResponse.from(
				orderSnapshotQueryService.getSnapshot(orderId, snapshotId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
