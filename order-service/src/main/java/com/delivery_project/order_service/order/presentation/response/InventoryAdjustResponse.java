package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryAdjustResult;

import java.util.UUID;

public record InventoryAdjustResponse(
		UUID inventoryId,
		Integer previousQuantity,
		Integer quantity,
		Integer adjustedDelta,
		Integer reservedQuantity,
		Integer availableQuantity,
		String reason
) {
	public static InventoryAdjustResponse from(InventoryAdjustResult result) {
		return new InventoryAdjustResponse(
				result.inventoryId(), result.previousQuantity(), result.quantity(), result.adjustedDelta(),
				result.reservedQuantity(), result.availableQuantity(), result.reason());
	}
}
