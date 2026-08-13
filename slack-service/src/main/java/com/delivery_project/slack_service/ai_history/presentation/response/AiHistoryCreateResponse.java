package com.delivery_project.slack_service.ai_history.presentation.response;

import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistoryCreateResponse(
        UUID aiHistoryId,
        UUID orderId,
        String modelName,
        Instant finalDispatchDeadline,
        AiHistoryStatus status,
        Instant requestedAt,
        Instant respondedAt
) {

    public static AiHistoryCreateResponse from(
            AiHistoryCreateResult result
    ) {
        return new AiHistoryCreateResponse(
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