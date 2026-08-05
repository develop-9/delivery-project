package com.delivery_project.slack_service.slack.domain.repository;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlackMessageQueryRepository {

    Optional<SlackMessage> findById(UUID slackMessageId);

    List<SlackMessage> findAll();
}