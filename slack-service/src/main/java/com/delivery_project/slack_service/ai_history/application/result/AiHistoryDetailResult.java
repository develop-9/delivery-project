package com.delivery_project.slack_service.ai_history.application.result;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistoryDetailResult(
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

    public static AiHistoryDetailResult from(
            AiHistory aiHistory
    ) {
        return new AiHistoryDetailResult(
                aiHistory.getId(),
                aiHistory.getOrderId(),
                aiHistory.getModelName(),
                aiHistory.getPrompt(),
                aiHistory.getFinalDispatchDeadline(),
                aiHistory.getStatus(),
                aiHistory.getRequestedAt(),
                aiHistory.getRespondedAt(),
                aiHistory.getCreatedAt(),
                aiHistory.getCreatedBy()
        );
    }
}