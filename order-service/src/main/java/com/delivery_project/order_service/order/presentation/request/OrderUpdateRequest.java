package com.delivery_project.order_service.order.presentation.request;

import com.delivery_project.order_service.order.application.command.OrderItemCommand;
import com.delivery_project.order_service.order.application.command.OrderUpdateCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 수정. 넘어온 필드만 반영한다(부분 수정).
 * items 가 null 이면 상품 구성은 그대로 두고, 비어 있지 않은 리스트가 오면 그 구성으로 맞춘다.
 */
public record OrderUpdateRequest(

		@Size(max = 500, message = "요청사항은 500자를 넘을 수 없습니다.")
		String requestDetails,

		@Future(message = "납품 기한은 현재 시각 이후여야 합니다.")
		LocalDateTime dueAt,

		@Size(min = 1, max = 20, message = "주문 상품은 1~20종이어야 합니다.")
		@Valid
		List<OrderCreateRequest.OrderItemRequest> items
) {
	public OrderUpdateCommand toCommand(UUID orderId) {
		List<OrderItemCommand> itemCommands = items == null
				? null
				: items.stream().map(OrderCreateRequest.OrderItemRequest::toCommand).toList();

		return new OrderUpdateCommand(orderId, requestDetails, dueAt, itemCommands);
	}
}
