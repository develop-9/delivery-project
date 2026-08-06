package com.delivery_project.slack_service.ai_history.application.command;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;

import java.time.Instant;
import java.util.UUID;

public record AiHistorySearchCommand(
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

    public static AiHistorySearchCommand of(
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
        return new AiHistorySearchCommand(
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