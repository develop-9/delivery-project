package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import com.delivery_project.slack_service.slack.application.command_service.SlackMessagePersistenceService;
import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import com.delivery_project.slack_service.slack.application.port.SlackMessageQueuePublisher;
import com.delivery_project.slack_service.slack.application.port.SlackMessageSender;
import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.infrastructure.config.SlackRabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackMessageQueueConsumer {

    private final SlackMessageSender slackMessageSender;
    private final SlackMessagePersistenceService slackMessagePersistenceService;
    private final SlackMessageQueuePublisher slackMessageQueuePublisher;
    private final SlackMessageDuplicateGuard slackMessageDuplicateGuard;

    @RabbitListener(queues = SlackRabbitMqConfig.MAIN_QUEUE)
    public void consume(
            SlackMessageQueuePayload payload
    ) {
        if (!slackMessageDuplicateGuard.tryAcquire(payload.slackMessageId())) {
            log.warn(
                    "Slack 메시지 중복 소비 감지. 발송을 건너뜁니다. slackMessageId={}",
                    payload.slackMessageId()
            );

            return;
        }

        try {
            processMessage(payload);
        } finally {
            slackMessageDuplicateGuard.release(payload.slackMessageId());
        }
    }

    private void processMessage(SlackMessageQueuePayload payload) {
        SlackMessageSendResult sendResult =
                slackMessageSender.send(
                        payload.receiverSlackId(),
                        payload.message()
                );

        if (sendResult.success()) {
            slackMessagePersistenceService.markSent(
                    payload.slackMessageId()
            );

            log.info(
                    "Slack 메시지 발송 성공. slackMessageId={}",
                    payload.slackMessageId()
            );

            return;
        }

        SlackMessage failedMessage =
                slackMessagePersistenceService.markFailed(
                        payload.slackMessageId(),
                        sendResult.errorMessage()
                );

        log.warn(
                "Slack 메시지 발송 실패. slackMessageId={}, retryCount={}, error={}",
                failedMessage.getId(),
                failedMessage.getRetryCount(),
                sendResult.errorMessage()
        );

        if (!failedMessage.canRetry()) {
            log.error(
                    "Slack 메시지 최대 재시도 횟수 초과. slackMessageId={}, retryCount={}",
                    failedMessage.getId(),
                    failedMessage.getRetryCount()
            );

            return;
        }

        SlackMessage retryMessage =
                slackMessagePersistenceService.prepareRetry(
                        payload.slackMessageId()
                );

        try {
            slackMessageQueuePublisher.publishRetry(
                    retryMessage.getId(),
                    payload.receiverSlackId(),
                    payload.message()
            );

            log.info(
                    "Slack 메시지 재시도 Queue 등록. slackMessageId={}, retryCount={}",
                    retryMessage.getId(),
                    retryMessage.getRetryCount()
            );

        } catch (Exception exception) {

            slackMessagePersistenceService.markFailed(
                    payload.slackMessageId(),
                    "Retry Queue 발행 실패: " + exception.getMessage()
            );

            log.error(
                    "Slack Retry Queue 발행 실패. slackMessageId={}",
                    payload.slackMessageId(),
                    exception
            );
        }
    }
}