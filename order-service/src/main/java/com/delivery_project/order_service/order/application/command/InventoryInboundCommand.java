package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.InventoryInboundRequest;

import java.util.UUID;

public record InventoryInboundCommand(
		UUID inventoryId,
		Integer quantity,
		String note
) {

	public static InventoryInboundCommand from(UUID inventoryId, InventoryInboundRequest request) {
		return new InventoryInboundCommand(inventoryId, request.quantity(), request.note());
	}
}
