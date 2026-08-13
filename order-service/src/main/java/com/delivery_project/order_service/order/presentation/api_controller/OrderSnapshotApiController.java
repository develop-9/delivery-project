package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.global.response.PageResponse;
import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.global.security.JwtPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 주문 이력(스냅샷) 조회.
 *
 * <p>주문과 이력은 관리하는 데이터와 응답 책임이 달라 컨트롤러를 분리한다.
 * 이력은 사용자가 만들거나 고치는 대상이 아니라 주문 변경 시 서버가 남기는 기록이므로 조회만 제공한다.
 *
 * <p>클래스 레벨 {@code @RequestMapping} 을 두지 않는다. API 명세상 두 조회가
 * <b>서로 다른 리소스 계층</b>에 있어서 공통 prefix 로 묶을 수 없다.
 * <ul>
 *   <li>타임라인 — {@code /api/v1/orders/{orderId}/order-snapshots} : 특정 주문에 종속된 하위 리소스</li>
 *   <li>단건 — {@code /api/v1/order-snapshots/{orderSnapshotId}} : 독립 리소스</li>
 * </ul>
 */
@Tag(name = "주문 이력", description = "주문 이력(스냅샷) 조회")
@RestController
@RequiredArgsConstructor
public class OrderSnapshotApiController {

	private final OrderSnapshotQueryService orderSnapshotQueryService;

	@Operation(summary = "주문 이력 타임라인",
			description = "특정 주문의 이력을 사건별로 반환한다. eventType 으로 걸러 볼 수 있다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "주문 없음 · 삭제됨")
	})
	@GetMapping("/api/v1/orders/{orderId}/order-snapshots")
	public ResponseEntity<SuccessResponse<PageResponse<OrderSnapshotResponse>>> getSnapshots(
			@AuthenticationPrincipal JwtPrincipal principal,
			@PathVariable UUID orderId,
			@RequestParam(required = false) EventType eventType,
			Pageable pageable
	) {
		PageResponse<OrderSnapshotResponse> response = PageResponse.of(
				orderSnapshotQueryService.getSnapshots(orderId, eventType, pageable, principal), OrderSnapshotResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "주문 이력 단건 조회",
			description = "이력 ID 로 직접 조회한다. 자기 주문의 이력만 볼 수 있고, "
					+ "남의 이력은 존재를 숨기기 위해 404 로 응답한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "이력 없음 · 삭제됨 · 조회 권한 없음")
	})
	@GetMapping("/api/v1/order-snapshots/{orderSnapshotId}")
	public ResponseEntity<SuccessResponse<OrderSnapshotResponse>> getSnapshot(
			@AuthenticationPrincipal JwtPrincipal principal,
			@PathVariable UUID orderSnapshotId
	) {
		OrderSnapshotResponse response = OrderSnapshotResponse.from(
				orderSnapshotQueryService.getSnapshot(orderSnapshotId, principal));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
