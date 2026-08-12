package com.delivery_project.order_service.order.infrastructure.client;

import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.ProductInfoResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", contextId = "productInternalClient")
public interface ProductInternalClient {

	@GetMapping("/internal/v1/products/{productId}")
	InternalApiResponse<ProductInfoResponse> getProduct(@PathVariable UUID productId);
}
