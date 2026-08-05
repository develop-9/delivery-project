package com.delivery_project.order_service.order.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderUpdateCommand(
		UUID orderId,
		String requestDetails,
		LocalDateTime dueAt,
		List<OrderItemCommand> items
) {
}
