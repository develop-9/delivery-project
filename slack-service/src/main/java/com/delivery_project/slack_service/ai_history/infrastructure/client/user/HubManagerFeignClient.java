package com.delivery_project.slack_service.ai_history.infrastructure.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        contextId = "hubManagerFeignClient"
)
public interface HubManagerFeignClient {

    @GetMapping("/internal/v1/users")
    UserApiResponse getHubManager(
            @RequestParam("hubId") UUID hubId,
            @RequestParam("role") String role
    );

    record UserApiResponse(
            boolean success,
            UserData data
    ) {
    }

    record UserData(
            UUID userId,
            String username,
            String name,
            String role,
            UUID hubId,
            UUID companyId
    ) {
    }
}