package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SlackMessagePersistenceService {

    private final SlackMessageCommandRepository slackMessageCommandRepository;
    private final UUID systemId;

    public SlackMessagePersistenceService(
            SlackMessageCommandRepository slackMessageCommandRepository,
            @Value("${system.id}") UUID systemId
    ) {
        this.slackMessageCommandRepository =
                slackMessageCommandRepository;
        this.systemId = systemId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage createPending(
            UUID receiverUserId,
            String receiverSlackId,
            String message
    ) {
        SlackMessage slackMessage = SlackMessage.create(
                systemId,
                SenderType.SYSTEM,
                receiverUserId,
                receiverSlackId,
                message
        );

        return slackMessageCommandRepository.save(slackMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage markSent(
            UUID slackMessageId
    ) {
        SlackMessage slackMessage =
                findSlackMessage(slackMessageId);

        slackMessage.markAsSent();

        return slackMessage;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage markFailed(
            UUID slackMessageId,
            String failureMessage
    ) {
        SlackMessage slackMessage =
                findSlackMessage(slackMessageId);

        slackMessage.markAsFailed(failureMessage);

        return slackMessage;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage prepareRetry(
            UUID slackMessageId
    ) {
        SlackMessage slackMessage =
                findSlackMessage(slackMessageId);

        slackMessage.prepareRetry();

        return slackMessage;
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