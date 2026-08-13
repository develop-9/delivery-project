package com.delivery_project.slack_service.ai_history.application.persistence_service;

import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryCommandRepository;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryQueryRepository;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiHistoryPersistenceService {

    private final AiHistoryCommandRepository aiHistoryCommandRepository;
    private final AiHistoryQueryRepository aiHistoryQueryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createPending(
            UUID orderId,
            String prompt,
            Instant requestedAt
    ) {
        AiHistory aiHistory = AiHistory.createPending(
                orderId,
                prompt,
                requestedAt
        );

        AiHistory savedAiHistory =
                aiHistoryCommandRepository.save(aiHistory);

        return savedAiHistory.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiHistory complete(
            UUID aiHistoryId,
            String modelName,
            Instant finalDispatchDeadline,
            Instant respondedAt
    ) {
        AiHistory aiHistory =
                findAiHistory(aiHistoryId);

        aiHistory.complete(
                modelName,
                finalDispatchDeadline,
                respondedAt
        );

        return aiHistoryCommandRepository.save(aiHistory);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            UUID aiHistoryId,
            String modelName,
            Instant respondedAt
    ) {
        AiHistory aiHistory =
                findAiHistory(aiHistoryId);

        aiHistory.fail(
                modelName,
                respondedAt
        );

        aiHistoryCommandRepository.save(aiHistory);
    }

    private AiHistory findAiHistory(
            UUID aiHistoryId
    ) {
        return aiHistoryQueryRepository
                .findById(aiHistoryId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AI_HISTORY_NOT_FOUND
                        )
                );
    }
}