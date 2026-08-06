package com.delivery_project.slack_service.slack.infrastructure.external.user;

import com.delivery_project.slack_service.global.response.SuccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserSlackIdFeignClient {

    @GetMapping("/internal/v1/users/{userId}/slack")
    SuccessResponse<UserSlackIdFeignResponse> getUserSlackId(
            @PathVariable("userId") UUID userId
    );
}