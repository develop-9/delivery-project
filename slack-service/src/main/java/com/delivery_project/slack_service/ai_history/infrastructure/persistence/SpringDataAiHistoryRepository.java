package com.delivery_project.slack_service.ai_history.infrastructure.persistence;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SpringDataAiHistoryRepository
        extends JpaRepository<AiHistory, UUID>,
        JpaSpecificationExecutor<AiHistory> {
}