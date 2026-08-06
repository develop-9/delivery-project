package com.delivery_project.slack_service.ai_history.application.port;

import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;

import java.util.UUID;

public interface DeliveryRouteClient {

    DeliveryRouteResult getRoutesByOrderId(
            UUID orderId
    );
}