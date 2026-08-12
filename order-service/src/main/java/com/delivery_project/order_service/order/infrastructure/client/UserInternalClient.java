package com.delivery_project.order_service.order.infrastructure.client;

import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.UserInfoResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserInternalClient {

	@GetMapping("/internal/v1/users/{userId}")
	InternalApiResponse<UserInfoResponse> getUser(@PathVariable UUID userId);
}
