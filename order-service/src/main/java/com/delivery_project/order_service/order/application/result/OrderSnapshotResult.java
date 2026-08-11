package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshotResult(
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
	}

	public static OrderSnapshotResult from(OrderSnapshot snapshot) {
		List<Item> items = snapshot.getItems().stream()
				.map(i -> new Item(i.getProductId(), i.getProductName(), i.getQuantity()))
				.toList();

		return new OrderSnapshotResult(
				snapshot.getId(), snapshot.getOrderId(), snapshot.getSequence(),
				snapshot.getEventType().name(), snapshot.getOrderStatus().name(),
				snapshot.getSupplierCompanyName(), snapshot.getReceiverCompanyName(),
				snapshot.getOriginHubName(), snapshot.getDestHubName(),
				items, snapshot.getRequestDetails(),
				snapshot.getCreatedAt(), snapshot.getCreatedBy());
	}
}
