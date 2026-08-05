package com.delivery_project.slack_service.slack.infrastructure.persistence;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SlackMessageRepositoryImpl
        implements SlackMessageCommandRepository, SlackMessageQueryRepository {

    private final SpringDataSlackMessageRepository springDataSlackMessageRepository;

    @Override
    public SlackMessage save(SlackMessage slackMessage) {
        return springDataSlackMessageRepository.save(slackMessage);
    }

    @Override
    public Optional<SlackMessage> findById(UUID slackMessageId) {
        return springDataSlackMessageRepository
                .findByIdAndDeletedAtIsNull(slackMessageId);
    }

    @Override
    public List<SlackMessage> findAll() {
        return springDataSlackMessageRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    }
}