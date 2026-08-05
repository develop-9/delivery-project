package com.delivery_project.slack_service.slack.application.query_service;

import com.delivery_project.slack_service.slack.application.result.SlackMessageQueryResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageQueryService {

    private final SlackMessageQueryRepository slackMessageQueryRepository;

    public SlackMessageQueryResult findById(UUID slackMessageId) {
        SlackMessage slackMessage = slackMessageQueryRepository
                .findById(slackMessageId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Slack 메시지를 찾을 수 없습니다."
                        )
                );

        return SlackMessageQueryResult.from(slackMessage);
    }

    public List<SlackMessageQueryResult> findAll() {
        return slackMessageQueryRepository
                .findAll()
                .stream()
                .map(SlackMessageQueryResult::from)
                .toList();
    }
}