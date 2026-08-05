package com.delivery_project.order_service.order.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemCommand(
		UUID productId,
		String productName,
		Integer quantity,
		BigDecimal unitPrice
) {
}
