package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryTransferResult;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransferResponse(
		UUID productId,
		Integer transferredQuantity,
		Side from,
		Side to,
		boolean toCreated,
		Instant transferredAt
) {
	public record Side(UUID inventoryId, UUID hubId, Integer quantity, Integer availableQuantity) {
		public static Side from(InventoryTransferResult.Side side) {
			return new Side(side.inventoryId(), side.hubId(), side.quantity(), side.availableQuantity());
		}
	}

	public static InventoryTransferResponse from(InventoryTransferResult result) {
		return new InventoryTransferResponse(
				result.productId(), result.transferredQuantity(),
				Side.from(result.from()), Side.from(result.to()),
				result.toCreated(), result.transferredAt());
	}
}
