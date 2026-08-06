package com.delivery_project.slack_service.ai_history.domain.repository;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.global.common.PageData;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AiHistoryQueryRepository {

    Optional<AiHistory> findById(UUID aiHistoryId);

    PageData<AiHistory> findAll(
            UUID orderId,
            AiHistoryStatus status,
            String modelName,
            Instant startDate,
            Instant endDate,
            int page,
            int size,
            String sortField,
            String sortDirection
    );
}