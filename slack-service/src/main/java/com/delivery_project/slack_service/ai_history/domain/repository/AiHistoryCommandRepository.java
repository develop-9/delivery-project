package com.delivery_project.slack_service.ai_history.domain.repository;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;

import java.util.Optional;
import java.util.UUID;

public interface AiHistoryCommandRepository {

    AiHistory save(AiHistory aiHistory);

    Optional<AiHistory> findById(UUID aiHistoryId);
}