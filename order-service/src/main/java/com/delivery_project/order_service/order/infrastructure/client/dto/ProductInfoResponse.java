package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.UUID;

/** company-service GET /internal/v1/products/{productId} 응답. */
public record ProductInfoResponse(
		UUID productId,
		String name,
		Integer price
) {
}
