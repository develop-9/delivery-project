package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * delivery-service {@code POST /internal/v1/deliveries} 요청 본문.
 * 상대 서비스의 {@code DeliveryCreateRequest} 와 필드명이 일치해야 한다.
 */
public record DeliveryCreateApiRequest(
		UUID orderId,
		UUID departureHubId,
		UUID destinationHubId,
		String deliveryAddress,
		UUID receiverUserId
) {
}
