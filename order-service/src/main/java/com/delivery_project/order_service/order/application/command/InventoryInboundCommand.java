package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record InventoryInboundCommand(
		UUID inventoryId,
		Integer quantity,
		String note
) {
}
