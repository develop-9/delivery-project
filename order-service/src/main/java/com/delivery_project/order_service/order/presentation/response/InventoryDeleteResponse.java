package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record InventoryDeleteResponse(
		UUID inventoryId,
		Integer remainingQuantity,
		Instant deletedAt,
		UUID deletedBy
) {
	public static InventoryDeleteResponse from(InventoryDeleteResult result) {
		return new InventoryDeleteResponse(
				result.inventoryId(), result.remainingQuantity(), result.deletedAt(), result.deletedBy());
	}
}
