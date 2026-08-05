package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 주문 상세 응답 — 접수·수정·취소·조회가 공유한다 */
public record OrderResponse(
		UUID orderId,
		String status,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID originHubId,
		UUID destHubId,
		UUID requesterUserId,
		Integer itemCount,
		Integer totalQuantity,
		BigDecimal totalPrice,
		List<Item> items,
		UUID deliveryId,
		String requestDetails,
		String cancelReason,
		LocalDateTime dueAt,
		LocalDateTime dispatchDeadlineAt,
		boolean dispatchOverdue,
		Instant createdAt,
		Instant updatedAt
) {
	public record Item(
			UUID orderItemId,
			UUID productId,
			String productName,
			Integer quantity,
			BigDecimal unitPrice,
			BigDecimal linePrice,
			UUID inventoryId
	) {
		public static Item from(OrderResult.Item item) {
			return new Item(item.orderItemId(), item.productId(), item.productName(),
					item.quantity(), item.unitPrice(), item.linePrice(), item.inventoryId());
		}
	}

	public static OrderResponse from(OrderResult result) {
		return new OrderResponse(
				result.orderId(), result.status(),
				result.supplierCompanyId(), result.receiverCompanyId(),
				result.originHubId(), result.destHubId(), result.requesterUserId(),
				result.itemCount(), result.totalQuantity(), result.totalPrice(),
				result.items().stream().map(Item::from).toList(),
				result.deliveryId(), result.requestDetails(), result.cancelReason(),
				result.dueAt(), result.dispatchDeadlineAt(), result.dispatchOverdue(),
				result.createdAt(), result.updatedAt());
	}
}
