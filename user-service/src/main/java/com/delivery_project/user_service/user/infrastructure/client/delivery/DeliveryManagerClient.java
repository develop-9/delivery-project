package com.delivery_project.user_service.user.infrastructure.client.delivery;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "delivery-service", path = "/internal/v1/delivery-managers")
public interface DeliveryManagerClient {

	@DeleteMapping("/users/{userId}")
	void deleteByUserId(@PathVariable UUID userId);
}
