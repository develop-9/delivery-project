package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.InventoryAdjustRequest;

import java.util.UUID;

public record InventoryAdjustCommand(
		UUID inventoryId,
		Integer quantity,
		String reason
) {

	public static InventoryAdjustCommand from(UUID inventoryId, InventoryAdjustRequest request) {
		return new InventoryAdjustCommand(inventoryId, request.quantity(), request.reason());
	}
}
