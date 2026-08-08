package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessageStatus;
import com.delivery_project.slack_service.slack.domain.repository.SlackMessageCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackMessagePersistenceServiceTest {

    @Mock
    private SlackMessageCommandRepository slackMessageCommandRepository;

    private final UUID systemId = UUID.randomUUID();

    private SlackMessagePersistenceService slackMessagePersistenceService;

    @BeforeEach
    void setUp() {
        slackMessagePersistenceService =
                new SlackMessagePersistenceService(slackMessageCommandRepository, systemId);
    }

    @Test
    void createPending은_SYSTEM_발신자로_PENDING_메시지를_저장한다() {
        // given
        UUID receiverUserId = UUID.randomUUID();
        when(slackMessageCommandRepository.save(any(SlackMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SlackMessage result = slackMessagePersistenceService.createPending(
                receiverUserId,
                "U1234567890",
                "테스트 메시지"
        );

        // then
        assertThat(result.getSenderUserId()).isEqualTo(systemId);
        assertThat(result.getSenderType()).isEqualTo(SenderType.SYSTEM);
        assertThat(result.getStatus()).isEqualTo(SlackMessageStatus.PENDING);
    }

    @Test
    void markSent은_대상_메시지를_찾아_SENT로_변경한다() {
        // given
        SlackMessage slackMessage = SlackMessage.create(
                systemId, SenderType.SYSTEM, UUID.randomUUID(), "U1234567890", "메시지"
        );
        when(slackMessageCommandRepository.findById(any()))
                .thenReturn(Optional.of(slackMessage));

        // when
        SlackMessage result = slackMessagePersistenceService.markSent(UUID.randomUUID());

        // then
        assertThat(result.getStatus()).isEqualTo(SlackMessageStatus.SENT);
    }

    @Test
    void 존재하지_않는_메시지를_조회하면_SLACK_MESSAGE_NOT_FOUND_예외가_발생한다() {
        // given
        when(slackMessageCommandRepository.findById(any()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> slackMessagePersistenceService.markSent(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.SLACK_MESSAGE_NOT_FOUND)
                );
    }
}
