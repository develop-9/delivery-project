package com.delivery_project.company_service.company.infrastructure.client.user;

import com.delivery_project.company_service.company.infrastructure.client.dto.response.UserFeignResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/internal/v1/users")
public interface UserClient {

    @GetMapping("/{userId}")
    SuccessResponse<UserFeignResponse> getCaller(
            @PathVariable UUID userId
    );
}
