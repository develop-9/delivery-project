package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshotResult(
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
	}

	public static OrderSnapshotResult from(OrderSnapshot snapshot) {
		List<Item> items = snapshot.getItems().stream()
				.map(i -> new Item(i.getLineNo(), i.getProductId(), i.getProductName(),
						i.getUnitPrice(), i.getQuantity(), i.getLinePrice()))
				.toList();

		return new OrderSnapshotResult(
				snapshot.getId(), snapshot.getOrderId(), snapshot.getSequence(),
				snapshot.getEventType().name(), snapshot.getOrderStatus().name(),
				snapshot.getSupplierCompanyId(), snapshot.getReceiverCompanyId(),
				snapshot.getOriginHubId(), snapshot.getDestHubId(),
				snapshot.getSupplierCompanyName(), snapshot.getReceiverCompanyName(),
				snapshot.getOriginHubName(), snapshot.getDestHubName(),
				snapshot.getDeliveryId(),
				snapshot.getItemCount(), snapshot.getTotalQuantity(), snapshot.getTotalPrice(), items,
				snapshot.getRequestDetails(), snapshot.getNote(),
				snapshot.getCreatedAt(), snapshot.getCreatedBy());
	}
}
