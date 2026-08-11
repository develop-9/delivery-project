package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.InventoryCreateRequest;

import java.util.UUID;

public record InventoryCreateCommand(
		UUID productId,
		UUID hubId,
		UUID companyId,
		Integer quantity
) {

	public static InventoryCreateCommand from(InventoryCreateRequest request) {
		return new InventoryCreateCommand(
				request.productId(), request.hubId(), request.companyId(), request.quantity());
	}
}
