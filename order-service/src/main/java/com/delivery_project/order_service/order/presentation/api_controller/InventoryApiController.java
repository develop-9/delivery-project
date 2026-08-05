package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.global.response.PageResponse;
import com.delivery_project.order_service.global.response.SuccessResponse;
import com.delivery_project.order_service.order.application.command_service.InventoryCommandService;
import com.delivery_project.order_service.order.application.query_service.InventoryQueryService;
import com.delivery_project.order_service.order.domain.repository.InventorySearchCondition;
import com.delivery_project.order_service.order.presentation.request.InventoryAdjustRequest;
import com.delivery_project.order_service.order.presentation.request.InventoryCreateRequest;
import com.delivery_project.order_service.order.presentation.request.InventoryInboundRequest;
import com.delivery_project.order_service.order.presentation.request.InventoryTransferRequest;
import com.delivery_project.order_service.order.presentation.response.InventoryAdjustResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryDeleteResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryInboundResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryResponse;
import com.delivery_project.order_service.order.presentation.response.InventoryTransferResponse;

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

@Tag(name = "재고", description = "재고 등록 · 조회 · 입고 · 보정 · 이관 · 삭제")
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryApiController {

	private final InventoryCommandService inventoryCommandService;
	private final InventoryQueryService inventoryQueryService;

	@Operation(summary = "재고 등록", description = "상품을 특정 허브에 배치한다. 수량 증가가 아니다(그건 입고).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "등록 성공"),
			@ApiResponse(responseCode = "409", description = "같은 상품·허브 재고가 이미 있음")
	})
	@PostMapping
	public ResponseEntity<SuccessResponse<InventoryResponse>> create(
			@Valid @RequestBody InventoryCreateRequest request
	) {
		InventoryResponse response = InventoryResponse.from(inventoryCommandService.create(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@Operation(summary = "재고 단건 조회", description = "보유 · 선점 · 가용 수량을 함께 반환한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "재고 없음 · 삭제됨")
	})
	@GetMapping("/{inventoryId}")
	public ResponseEntity<SuccessResponse<InventoryResponse>> getInventory(
			@PathVariable UUID inventoryId
	) {
		InventoryResponse response = InventoryResponse.from(inventoryQueryService.getInventory(inventoryId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "재고 검색", description = "상품 · 허브 · 업체 · 수량 · 가용 수량 조건과 페이징을 지원한다.")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<InventoryResponse>>> searchInventories(
			@ModelAttribute InventorySearchCondition condition,
			Pageable pageable
	) {
		PageResponse<InventoryResponse> response = PageResponse.of(
				inventoryQueryService.searchInventories(condition, pageable), InventoryResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "입고 처리", description = "보유 수량에 누적한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "입고 성공"),
			@ApiResponse(responseCode = "400", description = "입고 수량 오류"),
			@ApiResponse(responseCode = "404", description = "재고 없음")
	})
	@PostMapping("/{inventoryId}/inbound")
	public ResponseEntity<SuccessResponse<InventoryInboundResponse>> inbound(
			@PathVariable UUID inventoryId,
			@Valid @RequestBody InventoryInboundRequest request
	) {
		InventoryInboundResponse response = InventoryInboundResponse.from(
				inventoryCommandService.inbound(request.toCommand(inventoryId)));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "실사 재고 보정", description = "누적이 아니라 덮어쓰기다. 선점 수량 아래로는 내릴 수 없다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "보정 성공"),
			@ApiResponse(responseCode = "400", description = "사유 누락 · 선점 수량 미만"),
			@ApiResponse(responseCode = "404", description = "재고 없음")
	})
	@PatchMapping("/{inventoryId}/adjust")
	public ResponseEntity<SuccessResponse<InventoryAdjustResponse>> adjust(
			@PathVariable UUID inventoryId,
			@Valid @RequestBody InventoryAdjustRequest request
	) {
		InventoryAdjustResponse response = InventoryAdjustResponse.from(
				inventoryCommandService.adjust(request.toCommand(inventoryId)));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "허브 간 재고 이관", description = "단일 트랜잭션으로 처리된다. 가용 수량 범위 안에서만 가능하다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "이관 성공"),
			@ApiResponse(responseCode = "400", description = "출발·도착 허브 동일"),
			@ApiResponse(responseCode = "409", description = "가용 재고 부족")
	})
	@PostMapping("/transfer")
	public ResponseEntity<SuccessResponse<InventoryTransferResponse>> transfer(
			@Valid @RequestBody InventoryTransferRequest request
	) {
		InventoryTransferResponse response = InventoryTransferResponse.from(
				inventoryCommandService.transfer(request.toCommand()));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Operation(summary = "재고 삭제", description = "논리 삭제. 선점 수량이 있으면 차단된다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "400", description = "선점된 재고"),
			@ApiResponse(responseCode = "404", description = "재고 없음")
	})
	@DeleteMapping("/{inventoryId}")
	public ResponseEntity<SuccessResponse<InventoryDeleteResponse>> delete(
			@PathVariable UUID inventoryId
	) {
		InventoryDeleteResponse response = InventoryDeleteResponse.from(inventoryCommandService.delete(inventoryId));
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
