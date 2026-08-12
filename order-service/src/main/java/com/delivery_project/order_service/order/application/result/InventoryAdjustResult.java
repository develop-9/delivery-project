package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.UUID;

public record InventoryAdjustResult(
		UUID inventoryId,
		Integer previousQuantity,
		Integer quantity,
		Integer adjustedDelta,
		Integer reservedQuantity,
		Integer availableQuantity,
		String reason
) {
	public static InventoryAdjustResult of(Inventory inventory, int previousQuantity, String reason) {
		return new InventoryAdjustResult(inventory.getId(), previousQuantity, inventory.getQuantity(),
				inventory.getQuantity() - previousQuantity,
				inventory.getReservedQuantity(), inventory.getAvailableQuantity(), reason);
	}
}
