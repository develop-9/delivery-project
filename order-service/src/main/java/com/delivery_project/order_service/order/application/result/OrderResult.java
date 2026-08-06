package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 주문 상세 결과. 생성·수정·조회가 모두 같은 모양을 돌려주므로
 * 행동별 Result 를 따로 두지 않고 하나를 공유한다(동일 필드 중복 방지).
 */
public record OrderResult(
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
		public static Item from(OrderItem item) {
			return new Item(item.getId(), item.getProductId(), item.getQuantity(), item.getInventoryId());
		}
	}

	public static OrderResult from(Order order) {
		return new OrderResult(
				order.getId(),
				order.getStatus().name(),
				order.getSupplierCompanyId(),
				order.getReceiverCompanyId(),
				order.getReceiverUserId(),
				order.getRequestDetails(),
				order.getItems().stream().map(Item::from).toList(),
				order.getCreatedAt(),
				order.getUpdatedAt());
	}
}
