package com.delivery_project.slack_service.ai_history.application.result;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResult(
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
        UUID deliveryId,
        Instant createdAt,
        LatestSnapshotResult latestSnapshot
) {

    public record LatestSnapshotResult(
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