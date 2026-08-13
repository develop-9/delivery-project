package com.delivery_project.slack_service.ai_history.application.result;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistoryCreateResult(
        UUID aiHistoryId,
        UUID orderId,
        String modelName,
        Instant finalDispatchDeadline,
        AiHistoryStatus status,
        Instant requestedAt,
        Instant respondedAt
) {

    public static AiHistoryCreateResult from(
            AiHistory aiHistory
    ) {
        return new AiHistoryCreateResult(
                aiHistory.getId(),
                aiHistory.getOrderId(),
                aiHistory.getModelName(),
                aiHistory.getFinalDispatchDeadline(),
                aiHistory.getStatus(),
                aiHistory.getRequestedAt(),
                aiHistory.getRespondedAt()
        );
    }
}