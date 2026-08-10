package com.delivery_project.slack_service.slack.infrastructure.external.slack;

import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Slack 실제 API 연동 테스트")
class SlackMessageSenderIntegrationTest {

    @Test
    @DisplayName("실제 Slack 사용자에게 메시지를 발송한다")
    void send_realSlackApi() {
        // given
        String botToken =
                System.getenv("SLACK_BOT_TOKEN");

        String receiverSlackId =
                System.getenv("SLACK_TEST_RECEIVER_ID");

        assumeTrue(
                botToken != null && !botToken.isBlank(),
                "SLACK_BOT_TOKEN 환경변수가 없어 테스트를 건너뜁니다."
        );

        assumeTrue(
                receiverSlackId != null
                        && !receiverSlackId.isBlank(),
                "SLACK_TEST_RECEIVER_ID 환경변수가 없어 테스트를 건너뜁니다."
        );

        SlackMessageSenderImpl sender =
                new SlackMessageSenderImpl(
                        "https://slack.com/api",
                        botToken
                );

        String message =
                """
                [배송 프로젝트 Slack 연동 테스트]
                실제 Slack API 메시지 발송 테스트입니다.
                """;

        // when
        SlackMessageSendResult result =
                sender.send(
                        receiverSlackId,
                        message
                );

        // then
        assertThat(result)
                .isNotNull();

        assertThat(result.success())
                .isTrue();
    }
}