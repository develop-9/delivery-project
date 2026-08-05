package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.UUID;

public record InventoryInboundResult(
		UUID inventoryId,
		Integer inboundQuantity,
		Integer previousQuantity,
		Integer quantity,
		Integer reservedQuantity,
		Integer availableQuantity
) {
	public static InventoryInboundResult of(Inventory inventory, int inboundQuantity, int previousQuantity) {
		return new InventoryInboundResult(inventory.getId(), inboundQuantity, previousQuantity,
				inventory.getQuantity(), inventory.getReservedQuantity(), inventory.getAvailableQuantity());
	}
}
