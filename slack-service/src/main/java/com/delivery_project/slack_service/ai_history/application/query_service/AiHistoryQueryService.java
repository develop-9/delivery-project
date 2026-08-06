package com.delivery_project.slack_service.ai_history.application.query_service;

import com.delivery_project.slack_service.ai_history.application.command.AiHistorySearchCommand;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryListResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryQueryRepository;
import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiHistoryQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 30, 50);

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "requestedAt",
                    "respondedAt",
                    "createdAt",
                    "finalDispatchDeadline"
            );

    private static final Set<String> ALLOWED_SORT_DIRECTIONS =
            Set.of("ASC", "DESC");

    private final AiHistoryQueryRepository aiHistoryQueryRepository;

    public AiHistoryDetailResult findById(
            UUID aiHistoryId
    ) {
        AiHistory aiHistory = aiHistoryQueryRepository
                .findById(aiHistoryId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AI_HISTORY_NOT_FOUND
                        )
                );

        return AiHistoryDetailResult.from(aiHistory);
    }

    public PageData<AiHistoryListResult> findAll(
            AiHistorySearchCommand command
    ) {
        validatePage(command.page());
        validateDateRange(
                command.startDate(),
                command.endDate()
        );
        validateSortField(command.sortField());
        validateSortDirection(command.sortDirection());

        int validatedSize = normalizeSize(command.size());

        String validatedSortDirection =
                command.sortDirection().toUpperCase(Locale.ROOT);

        return aiHistoryQueryRepository
                .findAll(
                        command.orderId(),
                        command.status(),
                        command.modelName(),
                        command.startDate(),
                        command.endDate(),
                        command.page(),
                        validatedSize,
                        command.sortField(),
                        validatedSortDirection
                )
                .map(AiHistoryListResult::from);
    }

    private void validatePage(
            int page
    ) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }
    }

    private int normalizeSize(
            int size
    ) {
        if (ALLOWED_PAGE_SIZES.contains(size)) {
            return size;
        }

        return DEFAULT_PAGE_SIZE;
    }

    private void validateDateRange(
            java.time.Instant startDate,
            java.time.Instant endDate
    ) {
        if (
                startDate != null
                        && endDate != null
                        && startDate.isAfter(endDate)
        ) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }
    }

    private void validateSortField(
            String sortField
    ) {
        if (
                sortField == null
                        || !ALLOWED_SORT_FIELDS.contains(sortField)
        ) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }
    }

    private void validateSortDirection(
            String sortDirection
    ) {
        if (
                sortDirection == null
                        || !ALLOWED_SORT_DIRECTIONS.contains(
                        sortDirection.toUpperCase(Locale.ROOT)
                )
        ) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }
    }
}