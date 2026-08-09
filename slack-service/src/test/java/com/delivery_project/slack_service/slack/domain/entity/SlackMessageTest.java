package com.delivery_project.slack_service.slack.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlackMessageTest {

    private SlackMessage newSlackMessage() {
        return SlackMessage.create(
                UUID.randomUUID(),
                SenderType.SYSTEM,
                UUID.randomUUID(),
                "U1234567890",
                "테스트 메시지"
        );
    }

    @Test
    void 생성하면_PENDING_상태이고_retryCount는_0이다() {
        // when
        SlackMessage slackMessage = newSlackMessage();

        // then
        assertThat(slackMessage.getStatus()).isEqualTo(SlackMessageStatus.PENDING);
        assertThat(slackMessage.getRetryCount()).isZero();
    }

    @Test
    void 발송_성공_처리하면_SENT_상태가_되고_sentAt이_기록된다() {
        // given
        SlackMessage slackMessage = newSlackMessage();

        // when
        slackMessage.markAsSent();

        // then
        assertThat(slackMessage.getStatus()).isEqualTo(SlackMessageStatus.SENT);
        assertThat(slackMessage.getSentAt()).isNotNull();
        assertThat(slackMessage.getFailureMessage()).isNull();
    }

    @Test
    void 발송_실패_처리하면_FAILED_상태이고_실패사유가_기록된다() {
        // given
        SlackMessage slackMessage = newSlackMessage();

        // when
        slackMessage.markAsFailed("Slack API 오류");

        // then
        assertThat(slackMessage.getStatus()).isEqualTo(SlackMessageStatus.FAILED);
        assertThat(slackMessage.getFailureMessage()).isEqualTo("Slack API 오류");
    }

    @Test
    void 재시도를_준비하면_retryCount가_1씩_증가하고_PENDING으로_돌아간다() {
        // given
        SlackMessage slackMessage = newSlackMessage();
        slackMessage.markAsFailed("Slack API 오류");

        // when
        slackMessage.prepareRetry();

        // then
        assertThat(slackMessage.getRetryCount()).isEqualTo(1);
        assertThat(slackMessage.getStatus()).isEqualTo(SlackMessageStatus.PENDING);
        assertThat(slackMessage.getFailureMessage()).isNull();
    }

    @Test
    void 재시도_1차_2차_3차까지는_canRetry가_true이다() {
        // given
        SlackMessage slackMessage = newSlackMessage();

        // when & then
        slackMessage.prepareRetry();
        assertThat(slackMessage.getRetryCount()).isEqualTo(1);
        assertThat(slackMessage.canRetry()).isTrue();

        slackMessage.prepareRetry();
        assertThat(slackMessage.getRetryCount()).isEqualTo(2);
        assertThat(slackMessage.canRetry()).isTrue();

        slackMessage.prepareRetry();
        assertThat(slackMessage.getRetryCount()).isEqualTo(3);
        assertThat(slackMessage.canRetry()).isFalse();
    }

    @Test
    void 최대_재시도_횟수를_초과하면_prepareRetry_호출시_예외가_발생한다() {
        // given
        SlackMessage slackMessage = newSlackMessage();
        slackMessage.prepareRetry(); // 1
        slackMessage.prepareRetry(); // 2
        slackMessage.prepareRetry(); // 3

        // when & then
        assertThatThrownBy(slackMessage::prepareRetry)
                .isInstanceOf(IllegalStateException.class);
    }
}
