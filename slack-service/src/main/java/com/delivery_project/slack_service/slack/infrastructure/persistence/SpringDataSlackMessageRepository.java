package com.delivery_project.slack_service.slack.infrastructure.persistence;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataSlackMessageRepository
        extends JpaRepository<SlackMessage, UUID> {
}