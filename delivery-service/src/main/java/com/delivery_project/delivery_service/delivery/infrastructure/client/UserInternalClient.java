package com.delivery_project.delivery_service.delivery.infrastructure.client;

import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserInfoResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserSlackResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserInternalClient {

    @GetMapping("/internal/v1/users/{userId}")
    InternalApiResponse<UserInfoResponse> getUser(
            @PathVariable("userId") UUID userId
    );

    @GetMapping("/internal/v1/users/{userId}/slack")
    InternalApiResponse<UserSlackResponse> getUserSlack(
            @PathVariable("userId") UUID userId
    );
}