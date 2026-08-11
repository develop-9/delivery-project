package com.delivery_project.order_service.order.infrastructure.client;

import com.delivery_project.order_service.order.infrastructure.client.dto.HubIdsResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "hub-service")
public interface HubInternalClient {

	@GetMapping("/internal/v1/hubs/ids")
	InternalApiResponse<HubIdsResponse> getAllHubIds();
}
