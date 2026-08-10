package com.delivery_project.slack_service.ai_history.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubFeignClient {

    @GetMapping("/internal/v1/hubs")
    HubBatchApiResponse getHubs(
            @RequestParam("hubIds") List<UUID> hubIds
    );

    record HubBatchApiResponse(
            boolean success,
            String code,
            String message,
            HubBatchData data,
            Instant timestamp
    ) {
    }

    record HubBatchData(
            List<HubData> hubs,
            List<UUID> notFoundHubIds
    ) {
    }

    record HubData(
            UUID hubId,
            String name,
            String address
    ) {
    }
}