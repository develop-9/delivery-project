package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record InventoryTransferCommand(
		UUID productId,
		UUID fromHubId,
		UUID toHubId,
		Integer quantity,
		String note
) {
}
