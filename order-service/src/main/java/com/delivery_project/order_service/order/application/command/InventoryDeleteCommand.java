package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record InventoryDeleteCommand(
		UUID inventoryId,
		UUID deletedBy
) {
}
