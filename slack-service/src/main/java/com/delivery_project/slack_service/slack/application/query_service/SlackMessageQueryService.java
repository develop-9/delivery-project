package com.delivery_project.slack_service.slack.application.query_service;

import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.application.result.SlackMessageQueryResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 30, 50);

    private final SlackMessageQueryRepository slackMessageQueryRepository;

    public SlackMessageQueryResult findById(
            UUID slackMessageId
    ) {
        SlackMessage slackMessage =
                slackMessageQueryRepository
                        .findById(slackMessageId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.SLACK_MESSAGE_NOT_FOUND
                                )
                        );

        return SlackMessageQueryResult.from(slackMessage);
    }

    public PageData<SlackMessageQueryResult> findAll(
            int page,
            int size,
            String sortField,
            String sortDirection
    ) {
        validatePage(page);

        int validatedSize = validateSize(size);

        return slackMessageQueryRepository
                .findAll(
                        page,
                        validatedSize,
                        sortField,
                        sortDirection
                )
                .map(SlackMessageQueryResult::from);
    }

    private void validatePage(
            int page
    ) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }
    }

    private int validateSize(
            int size
    ) {
        if (ALLOWED_PAGE_SIZES.contains(size)) {
            return size;
        }

        return DEFAULT_PAGE_SIZE;
    }
}