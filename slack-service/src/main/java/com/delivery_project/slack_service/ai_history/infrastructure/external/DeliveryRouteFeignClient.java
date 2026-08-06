package com.delivery_project.slack_service.ai_history.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "delivery-service"
)
public interface DeliveryRouteFeignClient {

    @GetMapping("/internal/v1/deliveries/orders/{orderId}/routes")
    DeliveryRouteApiResponse getRoutesByOrderId(
            @PathVariable("orderId") UUID orderId
    );

    record DeliveryRouteApiResponse(
            boolean success,
            String code,
            String message,
            DeliveryRouteData data,
            Instant timestamp
    ) {
    }

    record DeliveryRouteData(
            UUID deliveryId,
            UUID orderId,
            List<RouteData> routes
    ) {
    }

    record RouteData(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            Integer estimatedDurationMin
    ) {
    }
}