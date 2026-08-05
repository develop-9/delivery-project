package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.slack.application.command.SlackMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.command.SlackMessageUpdateCommand;
import com.delivery_project.slack_service.slack.application.result.SlackMessageCreateResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageUpdateResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SlackMessageCommandService {

    private final SlackMessageCommandRepository slackMessageCommandRepository;

    public SlackMessageCreateResult create(
            SlackMessageCreateCommand command
    ) {
        SlackMessage slackMessage = SlackMessage.create(
                command.senderUserId(),
                command.senderType(),
                command.receiverUserId(),
                command.receiverSlackId(),
                command.message()
        );

        SlackMessage savedSlackMessage =
                slackMessageCommandRepository.save(slackMessage);

        return SlackMessageCreateResult.from(savedSlackMessage);
    }

    public SlackMessageUpdateResult update(
            UUID slackMessageId,
            SlackMessageUpdateCommand command
    ) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        slackMessage.updateMessage(command.message());

        return SlackMessageUpdateResult.from(slackMessage);
    }

    public void delete(
            UUID slackMessageId,
            UUID deletedBy
    ) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        slackMessage.delete(deletedBy);
    }

    private SlackMessage findSlackMessage(UUID slackMessageId) {
        return slackMessageCommandRepository
                .findById(slackMessageId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Slack 메시지를 찾을 수 없습니다."
                        )
                );
    }
}