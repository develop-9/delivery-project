package com.delivery_project.slack_service.slack.domain.repository;

import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageQueryRepository {

    Optional<SlackMessage> findById(UUID slackMessageId);

    PageData<SlackMessage> findAll(
            int page,
            int size,
            String sortField,
            String sortDirection
    );
}