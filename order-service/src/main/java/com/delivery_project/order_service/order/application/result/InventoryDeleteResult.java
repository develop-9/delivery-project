package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryDeleteResult(
		UUID inventoryId,
		Integer remainingQuantity,
		Instant deletedAt,
		UUID deletedBy
) {
	public static InventoryDeleteResult from(Inventory inventory) {
		return new InventoryDeleteResult(inventory.getId(), inventory.getQuantity(),
				inventory.getDeletedAt(), inventory.getDeletedBy());
	}
}
