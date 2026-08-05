package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record InventoryAdjustCommand(
		UUID inventoryId,
		Integer quantity,
		String reason
) {
}
