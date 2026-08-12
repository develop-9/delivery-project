package com.delivery_project.order_service.order.infrastructure.client;

import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiRequest;
import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "delivery-service")
public interface DeliveryInternalClient {

	@PostMapping("/internal/v1/deliveries")
	InternalApiResponse<DeliveryCreateApiResponse> createDelivery(
			@RequestBody DeliveryCreateApiRequest request);

	@PatchMapping("/internal/v1/deliveries/orders/{orderId}/cancel")
	InternalApiResponse<Void> cancelDelivery(@PathVariable UUID orderId);
}
