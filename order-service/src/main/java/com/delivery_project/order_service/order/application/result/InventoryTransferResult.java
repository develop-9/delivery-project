package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransferResult(
		UUID productId,
		Integer transferredQuantity,
		Side from,
		Side to,
		boolean toCreated,
		Instant transferredAt
) {
	public record Side(UUID inventoryId, UUID hubId, Integer quantity, Integer availableQuantity) {
		public static Side from(Inventory inventory) {
			return new Side(inventory.getId(), inventory.getHubId(),
					inventory.getQuantity(), inventory.getAvailableQuantity());
		}
	}

	public static InventoryTransferResult of(UUID productId, int quantity,
			Inventory from, Inventory to, boolean toCreated) {
		return new InventoryTransferResult(productId, quantity,
				Side.from(from), Side.from(to), toCreated, Instant.now());
	}
}
