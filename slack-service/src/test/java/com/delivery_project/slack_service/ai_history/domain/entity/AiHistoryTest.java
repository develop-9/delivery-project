package com.delivery_project.slack_service.ai_history.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI History Entity 테스트")
class AiHistoryTest {

    @Test
    @DisplayName("AI 요청 이력을 생성하면 PENDING 상태로 생성된다")
    void createPending_success() {
        // given
        UUID orderId = UUID.randomUUID();
        String prompt = "배송 마감 시각을 계산해주세요.";
        Instant requestedAt = Instant.parse("2026-08-09T01:00:00Z");

        // when
        AiHistory aiHistory = AiHistory.createPending(
                orderId,
                prompt,
                requestedAt
        );

        // then
        assertThat(aiHistory.getId()).isNotNull();
        assertThat(aiHistory.getOrderId()).isEqualTo(orderId);
        assertThat(aiHistory.getPrompt()).isEqualTo(prompt);
        assertThat(aiHistory.getStatus()).isEqualTo(AiHistoryStatus.PENDING);
        assertThat(aiHistory.getRequestedAt()).isEqualTo(requestedAt);

        assertThat(aiHistory.getModelName()).isNull();
        assertThat(aiHistory.getFinalDispatchDeadline()).isNull();
        assertThat(aiHistory.getRespondedAt()).isNull();
    }

    @Test
    @DisplayName("AI 요청이 성공하면 SUCCESS 상태와 응답 정보가 기록된다")
    void complete_success() {
        // given
        AiHistory aiHistory = AiHistory.createPending(
                UUID.randomUUID(),
                "배송 마감 시각을 계산해주세요.",
                Instant.parse("2026-08-09T01:00:00Z")
        );

        String modelName = "gemini-2.5-flash";
        Instant finalDispatchDeadline =
                Instant.parse("2026-08-09T06:00:00Z");
        Instant respondedAt =
                Instant.parse("2026-08-09T01:00:05Z");

        // when
        aiHistory.complete(
                modelName,
                finalDispatchDeadline,
                respondedAt
        );

        // then
        assertThat(aiHistory.getStatus()).isEqualTo(AiHistoryStatus.SUCCESS);
        assertThat(aiHistory.getModelName()).isEqualTo(modelName);
        assertThat(aiHistory.getFinalDispatchDeadline())
                .isEqualTo(finalDispatchDeadline);
        assertThat(aiHistory.getRespondedAt()).isEqualTo(respondedAt);
    }

    @Test
    @DisplayName("AI 요청이 실패하면 FAILED 상태와 응답 시각이 기록된다")
    void fail_success() {
        // given
        AiHistory aiHistory = AiHistory.createPending(
                UUID.randomUUID(),
                "배송 마감 시각을 계산해주세요.",
                Instant.parse("2026-08-09T01:00:00Z")
        );

        Instant respondedAt =
                Instant.parse("2026-08-09T01:00:05Z");

        // when
        aiHistory.fail(
                null,
                respondedAt
        );

        // then
        assertThat(aiHistory.getStatus()).isEqualTo(AiHistoryStatus.FAILED);
        assertThat(aiHistory.getModelName()).isNull();
        assertThat(aiHistory.getFinalDispatchDeadline()).isNull();
        assertThat(aiHistory.getRespondedAt()).isEqualTo(respondedAt);
    }
}