package com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq;

import com.delivery_project.slack_service.slack.application.command_service.SlackMessageCommandService;
import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import com.delivery_project.slack_service.slack.application.port.SlackMessageQueuePublisher;
import com.delivery_project.slack_service.slack.application.port.SlackMessageSender;
import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackMessageQueueConsumerTest {

    @Mock
    private SlackMessageSender slackMessageSender;

    @Mock
    private SlackMessageCommandService slackMessageCommandService;

    @Mock
    private SlackMessageQueuePublisher slackMessageQueuePublisher;

    @Mock
    private SlackMessageDuplicateGuard slackMessageDuplicateGuard;

    @InjectMocks
    private SlackMessageQueueConsumer slackMessageQueueConsumer;

    private final UUID slackMessageId = UUID.randomUUID();
    private final String receiverSlackId = "U1234567890";
    private final String message = "테스트 메시지";

    private final String lockToken = "test-lock-token";

    private SlackMessageQueuePayload payload() {
        return new SlackMessageQueuePayload(
                slackMessageId,
                receiverSlackId,
                message
        );
    }

    private SlackMessage newSlackMessage() {
        return SlackMessage.create(
                UUID.randomUUID(),
                SenderType.SYSTEM,
                UUID.randomUUID(),
                receiverSlackId,
                message
        );
    }

    @Test
    void 발송에_성공하면_SENT로_저장하고_재시도하지_않는다() {
        // given
        when(slackMessageDuplicateGuard.tryAcquire(slackMessageId))
                .thenReturn(lockToken);

        when(slackMessageSender.send(receiverSlackId, message))
                .thenReturn(SlackMessageSendResult.succeeded());

        // when
        slackMessageQueueConsumer.consume(payload());

        // then
        verify(slackMessageCommandService)
                .markSent(slackMessageId);

        verify(slackMessageCommandService, never())
                .markFailed(any(), any());

        verify(slackMessageQueuePublisher, never())
                .publishRetry(any(), any(), any());

        verify(slackMessageDuplicateGuard)
                .release(slackMessageId, lockToken);
    }

    @Test
    void 발송에_실패하고_재시도_가능하면_Retry_Queue에_발행한다() {
        // given
        SlackMessage failedMessage = newSlackMessage();
        failedMessage.markAsFailed("Slack API 오류");

        SlackMessage retryMessage = newSlackMessage();
        retryMessage.prepareRetry();

        when(slackMessageDuplicateGuard.tryAcquire(slackMessageId))
                .thenReturn(lockToken);

        when(slackMessageSender.send(receiverSlackId, message))
                .thenReturn(SlackMessageSendResult.failed("Slack API 오류"));

        when(slackMessageCommandService.markFailed(
                slackMessageId,
                "Slack API 오류"
        )).thenReturn(failedMessage);

        when(slackMessageCommandService.prepareRetry(slackMessageId))
                .thenReturn(retryMessage);

        // when
        slackMessageQueueConsumer.consume(payload());

        // then
        verify(slackMessageCommandService)
                .markFailed(slackMessageId, "Slack API 오류");

        verify(slackMessageCommandService)
                .prepareRetry(slackMessageId);

        verify(slackMessageQueuePublisher)
                .publishRetry(
                        retryMessage.getId(),
                        receiverSlackId,
                        message
                );

        verify(slackMessageDuplicateGuard)
                .release(slackMessageId, lockToken);
    }

    @Test
    void 최대_재시도_횟수를_초과하면_더이상_재시도하지_않는다() {
        // given
        SlackMessage failedMessage = newSlackMessage();

        failedMessage.prepareRetry(); // retryCount 1
        failedMessage.prepareRetry(); // retryCount 2
        failedMessage.prepareRetry(); // retryCount 3
        failedMessage.markAsFailed("Slack API 오류");

        when(slackMessageDuplicateGuard.tryAcquire(slackMessageId))
                .thenReturn(lockToken);

        when(slackMessageSender.send(receiverSlackId, message))
                .thenReturn(SlackMessageSendResult.failed("Slack API 오류"));

        when(slackMessageCommandService.markFailed(
                slackMessageId,
                "Slack API 오류"
        )).thenReturn(failedMessage);

        // when
        slackMessageQueueConsumer.consume(payload());

        // then
        verify(slackMessageCommandService, never())
                .prepareRetry(any());

        verify(slackMessageQueuePublisher, never())
                .publishRetry(any(), any(), any());

        verify(slackMessageDuplicateGuard)
                .release(slackMessageId, lockToken);
    }

    @Test
    void 중복_소비로_감지되면_발송을_시도하지_않는다() {
        // given
        when(slackMessageDuplicateGuard.tryAcquire(slackMessageId))
                .thenReturn(null);

        // when
        slackMessageQueueConsumer.consume(payload());

        // then
        verify(slackMessageSender, never())
                .send(any(), any());

        verify(slackMessageCommandService, never())
                .markSent(any());

        verify(slackMessageDuplicateGuard, never())
                .release(any(), any());
    }

    @Test
    void Retry_Queue_발행이_실패하면_최종_FAILED로_기록한다() {
        // given
        SlackMessage failedMessage = newSlackMessage();
        failedMessage.markAsFailed("Slack API 오류");

        SlackMessage retryMessage = newSlackMessage();
        retryMessage.prepareRetry();

        when(slackMessageDuplicateGuard.tryAcquire(slackMessageId))
                .thenReturn(lockToken);

        when(slackMessageSender.send(receiverSlackId, message))
                .thenReturn(SlackMessageSendResult.failed("Slack API 오류"));

        when(slackMessageCommandService.markFailed(
                slackMessageId,
                "Slack API 오류"
        )).thenReturn(failedMessage);

        when(slackMessageCommandService.prepareRetry(slackMessageId))
                .thenReturn(retryMessage);

        doThrow(new RuntimeException("연결 실패"))
                .when(slackMessageQueuePublisher)
                .publishRetry(
                        retryMessage.getId(),
                        receiverSlackId,
                        message
                );

        // when
        slackMessageQueueConsumer.consume(payload());

        // then
        verify(slackMessageCommandService)
                .markFailed(
                        eq(slackMessageId),
                        contains("Retry Queue 발행 실패")
                );

        verify(slackMessageDuplicateGuard)
                .release(slackMessageId, lockToken);
    }
}