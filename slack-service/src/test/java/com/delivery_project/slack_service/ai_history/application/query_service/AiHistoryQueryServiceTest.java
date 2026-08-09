package com.delivery_project.slack_service.ai_history.application.query_service;

import com.delivery_project.slack_service.ai_history.application.command.AiHistorySearchCommand;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryListResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryQueryRepository;
import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI History QueryService 테스트")
class AiHistoryQueryServiceTest {

    @Mock
    private AiHistoryQueryRepository aiHistoryQueryRepository;

    @InjectMocks
    private AiHistoryQueryService aiHistoryQueryService;

    @Test
    @DisplayName("AI 요청 이력을 ID로 단건 조회한다")
    void findById_success() {
        // given
        UUID aiHistoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-08-09T01:00:00Z");

        AiHistory aiHistory = AiHistory.createPending(
                orderId,
                "test prompt",
                requestedAt
        );

        when(aiHistoryQueryRepository.findById(aiHistoryId))
                .thenReturn(Optional.of(aiHistory));

        // when
        AiHistoryDetailResult result =
                aiHistoryQueryService.findById(aiHistoryId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo(AiHistoryStatus.PENDING);
        assertThat(result.prompt()).isEqualTo("test prompt");
        assertThat(result.requestedAt()).isEqualTo(requestedAt);

        verify(aiHistoryQueryRepository).findById(aiHistoryId);
    }

    @Test
    @DisplayName("존재하지 않는 AI 요청 이력을 조회하면 예외가 발생한다")
    void findById_notFound() {
        // given
        UUID aiHistoryId = UUID.randomUUID();

        when(aiHistoryQueryRepository.findById(aiHistoryId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> aiHistoryQueryService.findById(aiHistoryId)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(ErrorCode.AI_HISTORY_NOT_FOUND);
                });

        verify(aiHistoryQueryRepository).findById(aiHistoryId);
    }

    @Test
    @DisplayName("검색 조건으로 AI 요청 이력 목록을 조회한다")
    void findAll_success() {
        // given
        UUID orderId = UUID.randomUUID();
        Instant startDate = Instant.parse("2026-08-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-08-09T23:59:59Z");

        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        orderId,
                        AiHistoryStatus.SUCCESS,
                        "gemini-2.5-flash",
                        startDate,
                        endDate,
                        0,
                        30,
                        "requestedAt",
                        "desc"
                );

        AiHistory aiHistory = AiHistory.createPending(
                orderId,
                "test prompt",
                Instant.parse("2026-08-05T01:00:00Z")
        );

        PageData<AiHistory> pageData =
                new PageData<>(
                        List.of(aiHistory),
                        0,
                        30,
                        1L,
                        1
                );

        when(aiHistoryQueryRepository.findAll(
                orderId,
                AiHistoryStatus.SUCCESS,
                "gemini-2.5-flash",
                startDate,
                endDate,
                0,
                30,
                "requestedAt",
                "DESC"
        )).thenReturn(pageData);

        // when
        PageData<AiHistoryListResult> result =
                aiHistoryQueryService.findAll(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(30);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);

        verify(aiHistoryQueryRepository).findAll(
                orderId,
                AiHistoryStatus.SUCCESS,
                "gemini-2.5-flash",
                startDate,
                endDate,
                0,
                30,
                "requestedAt",
                "DESC"
        );
    }

    @Test
    @DisplayName("허용되지 않은 size 값은 기본값 10으로 정규화한다")
    void findAll_invalidSizeUsesDefault() {
        // given
        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        25,
                        "requestedAt",
                        "ASC"
                );

        PageData<AiHistory> pageData =
                new PageData<>(
                        List.of(),
                        0,
                        10,
                        0L,
                        0
                );

        when(aiHistoryQueryRepository.findAll(
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "requestedAt",
                "ASC"
        )).thenReturn(pageData);

        // when
        aiHistoryQueryService.findAll(command);

        // then
        verify(aiHistoryQueryRepository).findAll(
                null,
                null,
                null,
                null,
                null,
                0,
                10,
                "requestedAt",
                "ASC"
        );
    }

    @Test
    @DisplayName("page가 음수이면 검색 조건 예외가 발생한다")
    void findAll_invalidPage() {
        // given
        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        -1,
                        10,
                        "requestedAt",
                        "ASC"
                );

        // when & then
        assertInvalidSearchCondition(command);

        verifyNoInteractions(aiHistoryQueryRepository);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 검색 조건 예외가 발생한다")
    void findAll_invalidDateRange() {
        // given
        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        null,
                        null,
                        null,
                        Instant.parse("2026-08-10T00:00:00Z"),
                        Instant.parse("2026-08-09T00:00:00Z"),
                        0,
                        10,
                        "requestedAt",
                        "ASC"
                );

        // when & then
        assertInvalidSearchCondition(command);

        verifyNoInteractions(aiHistoryQueryRepository);
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드이면 검색 조건 예외가 발생한다")
    void findAll_invalidSortField() {
        // given
        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "id",
                        "ASC"
                );

        // when & then
        assertInvalidSearchCondition(command);

        verifyNoInteractions(aiHistoryQueryRepository);
    }

    @Test
    @DisplayName("허용되지 않은 정렬 방향이면 검색 조건 예외가 발생한다")
    void findAll_invalidSortDirection() {
        // given
        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "requestedAt",
                        "UP"
                );

        // when & then
        assertInvalidSearchCondition(command);

        verifyNoInteractions(aiHistoryQueryRepository);
    }

    private void assertInvalidSearchCondition(
            AiHistorySearchCommand command
    ) {
        assertThatThrownBy(
                () -> aiHistoryQueryService.findAll(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
                            );
                });
    }
}