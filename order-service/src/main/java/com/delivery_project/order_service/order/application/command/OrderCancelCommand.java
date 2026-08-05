package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

public record OrderCancelCommand(
		UUID orderId,
		String cancelReason
) {
}
