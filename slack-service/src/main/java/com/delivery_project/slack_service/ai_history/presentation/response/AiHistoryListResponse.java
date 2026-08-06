package com.delivery_project.slack_service.ai_history.presentation.response;

import com.delivery_project.slack_service.ai_history.application.result.AiHistoryListResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistoryListResponse(
        UUID aiHistoryId,
        UUID orderId,
        String modelName,
        Instant finalDispatchDeadline,
        AiHistoryStatus status,
        Instant requestedAt,
        Instant respondedAt
) {

    public static AiHistoryListResponse from(
            AiHistoryListResult result
    ) {
        return new AiHistoryListResponse(
                result.aiHistoryId(),
                result.orderId(),
                result.modelName(),
                result.finalDispatchDeadline(),
                result.status(),
                result.requestedAt(),
                result.respondedAt()
        );
    }
}