package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.OrderUpdateRequest;

import java.util.List;
import java.util.UUID;

/** 주문 수정 Use Case 입력. items 가 null 이면 상품 구성은 그대로 둔다. */
public record OrderUpdateCommand(
		UUID orderId,
		String requestDetails,
		List<OrderItemCommand> items
) {

	public static OrderUpdateCommand from(UUID orderId, OrderUpdateRequest request) {
		List<OrderItemCommand> items = request.items() == null
				? null
				: request.items().stream().map(OrderItemCommand::from).toList();

		return new OrderUpdateCommand(orderId, request.requestDetails(), items);
	}
}
