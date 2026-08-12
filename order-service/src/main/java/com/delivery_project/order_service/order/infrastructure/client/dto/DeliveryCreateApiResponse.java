package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * delivery-service 배송 생성 응답. order 는 {@code deliveryId} 만 쓰므로 나머지는 받지 않는다.
 */
public record DeliveryCreateApiResponse(
		UUID deliveryId
) {
}
