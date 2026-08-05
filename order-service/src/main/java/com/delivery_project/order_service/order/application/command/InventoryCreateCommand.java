package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record InventoryCreateCommand(
		UUID productId,
		UUID hubId,
		UUID companyId,
		Integer quantity
) {
}
