package com.delivery_project.slack_service.ai_history.application.port;

import com.delivery_project.slack_service.ai_history.application.result.OrderSummaryResult;

import java.util.UUID;

public interface OrderSummaryClient {

    OrderSummaryResult getOrderSummary(
            UUID orderId
    );
}