package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshotResponse(
		UUID snapshotId,
		UUID orderId,
		Integer sequence,
		String eventType,
		String orderStatus,
		String supplierCompanyName,
		String receiverCompanyName,
		String originHubName,
		String destHubName,
		List<Item> items,
		String requestDetails,
		Instant createdAt,
		UUID createdBy
) {
	public record Item(
			UUID productId,
			String productName,
			Integer quantity
	) {
		public static Item from(OrderSnapshotResult.Item item) {
			return new Item(item.productId(), item.productName(), item.quantity());
		}
	}

	public static OrderSnapshotResponse from(OrderSnapshotResult result) {
		return new OrderSnapshotResponse(
				result.snapshotId(), result.orderId(), result.sequence(),
				result.eventType(), result.orderStatus(),
				result.supplierCompanyName(), result.receiverCompanyName(),
				result.originHubName(), result.destHubName(),
				result.items().stream().map(Item::from).toList(),
				result.requestDetails(),
				result.createdAt(), result.createdBy());
	}
}
