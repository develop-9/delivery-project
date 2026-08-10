package com.delivery_project.slack_service.ai_history.application.result;

import java.util.List;
import java.util.UUID;

public record OrderSummaryResult(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        String requestDetails,
        List<ItemResult> items
) {

    public record ItemResult(
            UUID productId,
            Integer quantity
    ) {
    }
}