package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 주문 상세 응답 — 접수·수정·조회가 공유한다 */
public record OrderResponse(
		UUID orderId,
		String status,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID receiverUserId,
		String requestDetails,
		List<Item> items,
		Instant createdAt,
		Instant updatedAt
) {
	public record Item(
			UUID orderItemId,
			UUID productId,
			Integer quantity,
			UUID inventoryId
	) {
		public static Item from(OrderResult.Item item) {
			return new Item(item.orderItemId(), item.productId(), item.quantity(), item.inventoryId());
		}
	}

	public static OrderResponse from(OrderResult result) {
		return new OrderResponse(
				result.orderId(), result.status(),
				result.supplierCompanyId(), result.receiverCompanyId(), result.receiverUserId(),
				result.requestDetails(),
				result.items().stream().map(Item::from).toList(),
				result.createdAt(), result.updatedAt());
	}
}
