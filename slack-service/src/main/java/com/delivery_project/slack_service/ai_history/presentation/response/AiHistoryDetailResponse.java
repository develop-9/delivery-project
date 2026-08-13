package com.delivery_project.slack_service.ai_history.presentation.response;

import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistoryDetailResponse(
        UUID aiHistoryId,
        UUID orderId,
        String modelName,
        String prompt,
        Instant finalDispatchDeadline,
        AiHistoryStatus status,
        Instant requestedAt,
        Instant respondedAt,
        Instant createdAt,
        UUID createdBy
) {

    public static AiHistoryDetailResponse from(
            AiHistoryDetailResult result
    ) {
        return new AiHistoryDetailResponse(
                result.aiHistoryId(),
                result.orderId(),
                result.modelName(),
                result.prompt(),
                result.finalDispatchDeadline(),
                result.status(),
                result.requestedAt(),
                result.respondedAt(),
                result.createdAt(),
                result.createdBy()
        );
    }
}