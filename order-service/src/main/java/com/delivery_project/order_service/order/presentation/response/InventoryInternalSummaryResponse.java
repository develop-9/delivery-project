package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;

import java.util.UUID;

/** 내부 API 재고 응답. 단건 생성과 배치 조회가 같은 모양을 공유한다. */
public record InventoryInternalSummaryResponse(
		UUID inventoryId,
		UUID productId,
		UUID hubId,
		Integer quantity,
		Integer availableQuantity
) {
	public static InventoryInternalSummaryResponse from(InventoryInternalSummaryResult result) {
		return new InventoryInternalSummaryResponse(
				result.inventoryId(),
				result.productId(),
				result.hubId(),
				result.quantity(),
				result.availableQuantity());
	}
}
