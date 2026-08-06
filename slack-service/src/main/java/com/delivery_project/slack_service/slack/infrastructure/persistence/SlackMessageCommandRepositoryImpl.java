package com.delivery_project.slack_service.slack.infrastructure.persistence;

import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SlackMessageCommandRepositoryImpl
        implements SlackMessageCommandRepository {

    private final SpringDataSlackMessageRepository springDataSlackMessageRepository;

    @Override
    public SlackMessage save(
            SlackMessage slackMessage
    ) {
        return springDataSlackMessageRepository.save(slackMessage);
    }

    @Override
    public Optional<SlackMessage> findById(
            UUID slackMessageId
    ) {
        return springDataSlackMessageRepository.findById(slackMessageId);
    }
}