package com.delivery_project.slack_service.ai_history.application.query;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistorySearchQuery(
        UUID orderId,
        AiHistoryStatus status,
        String modelName,
        Instant startDate,
        Instant endDate,
        int page,
        int size,
        String sortField,
        String sortDirection
) {

    public static AiHistorySearchQuery of(
            UUID orderId,
            AiHistoryStatus status,
            String modelName,
            Instant startDate,
            Instant endDate,
            int page,
            int size,
            String sortField,
            String sortDirection
    ) {
        return new AiHistorySearchQuery(
                orderId,
                status,
                modelName,
                startDate,
                endDate,
                page,
                size,
                sortField,
                sortDirection
        );
    }
}