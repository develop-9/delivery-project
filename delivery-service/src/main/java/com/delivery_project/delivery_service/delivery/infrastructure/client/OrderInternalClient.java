package com.delivery_project.delivery_service.delivery.infrastructure.client;

import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.OrderInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderInternalClient {

    @GetMapping("/internal/v1/orders/{orderId}")
    InternalApiResponse<OrderInfoResponse> getOrder(
            @PathVariable("orderId") UUID orderId
    );
}
