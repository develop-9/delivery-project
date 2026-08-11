package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryInboundResult;

import java.util.UUID;

public record InventoryInboundResponse(
		UUID inventoryId,
		Integer inboundQuantity,
		Integer previousQuantity,
		Integer quantity,
		Integer reservedQuantity,
		Integer availableQuantity
) {
	public static InventoryInboundResponse from(InventoryInboundResult result) {
		return new InventoryInboundResponse(
				result.inventoryId(), result.inboundQuantity(), result.previousQuantity(),
				result.quantity(), result.reservedQuantity(), result.availableQuantity());
	}
}
