package com.delivery_project.delivery_service.delivery.infrastructure.client;

import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.HubRoutePathResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubInternalClient {

    @GetMapping("/internal/v1/hub-routes/path")
    InternalApiResponse<HubRoutePathResponse> getDeliveryRoutePath(
            @RequestHeader("Authorization") String authorization,
            @RequestParam UUID departureHubId,
            @RequestParam UUID arrivalHubId
    );
}
