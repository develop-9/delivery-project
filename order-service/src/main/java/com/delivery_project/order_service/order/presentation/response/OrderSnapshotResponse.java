package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshotResponse(
		UUID snapshotId,
		UUID orderId,
		Integer sequence,
		String eventType,
		String orderStatus,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID originHubId,
		UUID destHubId,
		String supplierCompanyName,
		String receiverCompanyName,
		String originHubName,
		String destHubName,
		UUID deliveryId,
		Integer itemCount,
		Integer totalQuantity,
		BigDecimal totalPrice,
		List<Item> items,
		String requestDetails,
		String note,
		Instant createdAt,
		UUID createdBy
) {
	public record Item(
			Integer lineNo,
			UUID productId,
			String productName,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal linePrice
	) {
		public static Item from(OrderSnapshotResult.Item item) {
			return new Item(item.lineNo(), item.productId(), item.productName(),
					item.unitPrice(), item.quantity(), item.linePrice());
		}
	}

	public static OrderSnapshotResponse from(OrderSnapshotResult result) {
		return new OrderSnapshotResponse(
				result.snapshotId(), result.orderId(), result.sequence(),
				result.eventType(), result.orderStatus(),
				result.supplierCompanyId(), result.receiverCompanyId(),
				result.originHubId(), result.destHubId(),
				result.supplierCompanyName(), result.receiverCompanyName(),
				result.originHubName(), result.destHubName(),
				result.deliveryId(),
				result.itemCount(), result.totalQuantity(), result.totalPrice(),
				result.items().stream().map(Item::from).toList(),
				result.requestDetails(), result.note(),
				result.createdAt(), result.createdBy());
	}
}
