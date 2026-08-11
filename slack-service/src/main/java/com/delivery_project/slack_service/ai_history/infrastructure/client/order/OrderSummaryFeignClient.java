package com.delivery_project.slack_service.ai_history.infrastructure.client.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

@FeignClient(
        name = "order-service"
)
public interface OrderSummaryFeignClient {

    @GetMapping("/internal/v1/orders/{orderId}")
    OrderSummaryApiResponse getOrderSummary(
            @PathVariable("orderId") UUID orderId,
            @RequestParam("include") String include
    );

    record OrderSummaryApiResponse(
            boolean success,
            String code,
            String message,
            OrderSummaryData data,
            Instant timestamp
    ) {
    }

    record OrderSummaryData(
            UUID orderId,
            String status,
            UUID productId,
            String productName,
            Integer quantity,
            UUID supplierCompanyId,
            String supplierCompanyName,
            UUID receiverCompanyId,
            String receiverCompanyName,
            UUID originHubId,
            UUID destHubId,
            String originHubName,
            String destHubName,
            UUID requesterUserId,
            String requesterName,
            String requestDetails,
            Instant dueAt,
            Instant dispatchDeadlineAt,
            UUID deliveryId,
            Instant createdAt,
            LatestSnapshotData latestSnapshot
    ) {
    }

    record LatestSnapshotData(
            UUID snapshotId,
            Integer sequence,
            String eventType,
            String productName,
            Integer quantity,
            String supplierCompanyName,
            String receiverCompanyName,
            String originHubName,
            String destHubName,
            String requestDetails,
            String orderStatus,
            String note,
            Instant createdAt
    ) {
    }
}