package com.delivery_project.slack_service.slack.domain.repository;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageCommandRepository {

    SlackMessage save(SlackMessage slackMessage);

    Optional<SlackMessage> findById(UUID slackMessageId);
}