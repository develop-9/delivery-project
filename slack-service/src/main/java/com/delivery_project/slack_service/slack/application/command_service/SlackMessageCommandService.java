package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.slack.application.command.SlackMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.command.SlackMessageUpdateCommand;
import com.delivery_project.slack_service.slack.application.result.SlackMessageCreateResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageDeleteResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageUpdateResult;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageQueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class SlackMessageCommandService {

    private final SlackMessageCommandRepository slackMessageCommandRepository;
    private final SlackMessageQueryRepository slackMessageQueryRepository;
    private final UUID systemId;

    public SlackMessageCommandService(
            SlackMessageCommandRepository slackMessageCommandRepository,
            SlackMessageQueryRepository slackMessageQueryRepository,
            @Value("${system.id}") UUID systemId
    ) {
        this.slackMessageCommandRepository = slackMessageCommandRepository;
        this.slackMessageQueryRepository = slackMessageQueryRepository;
        this.systemId = systemId;
    }

    public SlackMessageCreateResult create(
            SlackMessageCreateCommand command
    ) {
        UUID senderUserId;

        if (command.senderType() == SenderType.SYSTEM) {
            senderUserId = systemId;
        } else {
            if (command.senderUserId() == null) {
                throw new IllegalArgumentException(
                        "USER 발송 시 senderUserId는 필수입니다."
                );
            }

            senderUserId = command.senderUserId();
        }

        SlackMessage slackMessage = SlackMessage.create(
                senderUserId,
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

    public SlackMessageDeleteResult delete(
            UUID slackMessageId,
            UUID deletedBy
    ) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        slackMessage.delete(deletedBy);

        return SlackMessageDeleteResult.from(slackMessage);
    }

    private SlackMessage findSlackMessage(UUID slackMessageId) {
        return slackMessageQueryRepository
                .findById(slackMessageId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Slack 메시지를 찾을 수 없습니다."
                        )
                );
    }
}