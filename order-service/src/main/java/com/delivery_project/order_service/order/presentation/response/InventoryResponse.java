package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryResult;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
		UUID inventoryId,
		UUID productId,
		UUID hubId,
		UUID companyId,
		Integer quantity,
		Integer reservedQuantity,
		Integer availableQuantity,
		Instant createdAt,
		Instant updatedAt
) {
	public static InventoryResponse from(InventoryResult result) {
		return new InventoryResponse(
				result.inventoryId(), result.productId(), result.hubId(), result.companyId(),
				result.quantity(), result.reservedQuantity(), result.availableQuantity(),
				result.createdAt(), result.updatedAt());
	}
}
