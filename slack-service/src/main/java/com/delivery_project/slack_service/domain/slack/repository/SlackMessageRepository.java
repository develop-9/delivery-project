package com.delivery_project.slack_service.domain.slack.repository;

import com.delivery_project.slack_service.domain.slack.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {

    Optional<SlackMessage> findByIdAndDeletedAtIsNull(UUID id);

    List<SlackMessage> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}