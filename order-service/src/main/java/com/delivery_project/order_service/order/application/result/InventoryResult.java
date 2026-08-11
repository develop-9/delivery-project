package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryResult(
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
	public static InventoryResult from(Inventory inventory) {
		return new InventoryResult(
				inventory.getId(), inventory.getProductId(), inventory.getHubId(), inventory.getCompanyId(),
				inventory.getQuantity(), inventory.getReservedQuantity(), inventory.getAvailableQuantity(),
				inventory.getCreatedAt(), inventory.getUpdatedAt());
	}
}
