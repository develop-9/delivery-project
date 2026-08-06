package com.delivery_project.slack_service.slack.domain.repository;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;

public interface SlackMessageCommandRepository {

    SlackMessage save(SlackMessage slackMessage);
}