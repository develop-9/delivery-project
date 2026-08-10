package com.delivery_project.user_service.user.infrastructure.client.hub;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service", path = "/internal/v1/hubs")
public interface HubClient {

	@GetMapping("/{hubId}")
	void getHub(@PathVariable UUID hubId);
}
