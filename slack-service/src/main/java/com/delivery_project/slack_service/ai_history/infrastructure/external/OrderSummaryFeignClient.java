package com.delivery_project.slack_service.ai_history.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "order-service"
)
public interface OrderSummaryFeignClient {

    @GetMapping("/internal/v1/orders/{orderId}")
    OrderSummaryApiResponse getOrderSummary(
            @PathVariable("orderId") UUID orderId
    );

    record OrderSummaryApiResponse(
            boolean success,
            OrderSummaryData data
    ) {
    }

    record OrderSummaryData(
            UUID orderId,
            UUID supplierCompanyId,
            UUID receiverCompanyId,
            String requestDetails,
            List<ItemData> items
    ) {
    }

    record ItemData(
            UUID productId,
            Integer quantity
    ) {
    }
}