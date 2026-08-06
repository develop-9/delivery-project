package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.application.command.SlackMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.command.SlackMessageDeleteCommand;
import com.delivery_project.slack_service.slack.application.command.SlackMessageUpdateCommand;
import com.delivery_project.slack_service.slack.application.result.SlackMessageCreateResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageDeleteResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageUpdateResult;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SlackMessageCommandService {

    private final SlackMessageCommandRepository slackMessageCommandRepository;
    private final UUID systemId;

    public SlackMessageCommandService(
            SlackMessageCommandRepository slackMessageCommandRepository,
            @Value("${system.id}") UUID systemId
    ) {
        this.slackMessageCommandRepository =
                slackMessageCommandRepository;
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
                throw new BusinessException(
                        ErrorCode.SLACK_MESSAGE_SENDER_REQUIRED
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
            SlackMessageUpdateCommand command
    ) {
        SlackMessage slackMessage =
                findSlackMessage(command.slackMessageId());

        slackMessage.updateMessage(command.message());

        return SlackMessageUpdateResult.from(slackMessage);
    }

    public SlackMessageDeleteResult delete(
            SlackMessageDeleteCommand command
    ) {
        SlackMessage slackMessage =
                findSlackMessage(command.slackMessageId());

        slackMessage.delete(command.deletedBy());

        return SlackMessageDeleteResult.from(slackMessage);
    }

    private SlackMessage findSlackMessage(
            UUID slackMessageId
    ) {
        return slackMessageCommandRepository
                .findById(slackMessageId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.SLACK_MESSAGE_NOT_FOUND
                        )
                );
    }
}