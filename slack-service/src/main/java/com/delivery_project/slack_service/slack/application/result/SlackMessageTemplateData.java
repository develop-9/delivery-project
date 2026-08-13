package com.delivery_project.slack_service.slack.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SlackMessageTemplateData(
        UUID orderId,
        String ordererName,
        String ordererSlackId,
        Instant orderedAt,
        String productName,
        String requestMessage,
        String originHubName,
        List<String> routeHubNames,
        String destinationAddress,
        String deliveryManagerName,
        String deliveryManagerSlackId,
        Instant finalDispatchDeadline
) {
}