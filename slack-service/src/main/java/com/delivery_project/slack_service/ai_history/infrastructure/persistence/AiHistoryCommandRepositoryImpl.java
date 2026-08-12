package com.delivery_project.slack_service.ai_history.infrastructure.persistence;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiHistoryCommandRepositoryImpl
        implements AiHistoryCommandRepository {

    private final SpringDataAiHistoryRepository springDataAiHistoryRepository;

    @Override
    public AiHistory save(
            AiHistory aiHistory
    ) {
        return springDataAiHistoryRepository.save(aiHistory);
    }
}