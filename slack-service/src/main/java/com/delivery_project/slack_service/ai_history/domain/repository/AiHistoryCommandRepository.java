package com.delivery_project.slack_service.ai_history.domain.repository;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;

public interface AiHistoryCommandRepository {

    AiHistory save(AiHistory aiHistory);
}