package com.delivery_project.company_service.company.infrastructure.client.hub;

import com.delivery_project.company_service.company.infrastructure.client.dto.HubFeignResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/internal/v1/hubs/{hubId}")
    SuccessResponse<HubFeignResponse> getHub(
            @PathVariable UUID hubId
    );
}
