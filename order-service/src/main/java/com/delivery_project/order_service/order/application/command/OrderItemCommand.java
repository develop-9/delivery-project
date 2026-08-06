package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.OrderItemRequest;

import java.util.UUID;

/** 주문 상품 줄 하나. 상품명·단가는 company-service 소유라 여기 담지 않는다. */
public record OrderItemCommand(
		UUID productId,
		Integer quantity
) {

	public static OrderItemCommand from(OrderItemRequest request) {
		return new OrderItemCommand(request.productId(), request.quantity());
	}
}
