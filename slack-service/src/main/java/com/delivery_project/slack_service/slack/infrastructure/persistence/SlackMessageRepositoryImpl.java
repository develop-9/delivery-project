package com.delivery_project.slack_service.slack.infrastructure.persistence;

import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SlackMessageRepositoryImpl
        implements SlackMessageCommandRepository,
        SlackMessageQueryRepository {

    private final SpringDataSlackMessageRepository springDataSlackMessageRepository;

    @Override
    public SlackMessage save(SlackMessage slackMessage) {
        return springDataSlackMessageRepository.save(slackMessage);
    }

    @Override
    public Optional<SlackMessage> findById(UUID slackMessageId) {
        return springDataSlackMessageRepository.findById(slackMessageId);
    }

    @Override
    public PageData<SlackMessage> findAll(
            int page,
            int size,
            String sortField,
            String sortDirection
    ) {
        Sort.Direction direction =
                Sort.Direction.fromString(sortDirection);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );

        Page<SlackMessage> slackMessagePage =
                springDataSlackMessageRepository.findAll(pageRequest);

        return new PageData<>(
                slackMessagePage.getContent(),
                slackMessagePage.getNumber(),
                slackMessagePage.getSize(),
                slackMessagePage.getTotalElements(),
                slackMessagePage.getTotalPages()
        );
    }
}