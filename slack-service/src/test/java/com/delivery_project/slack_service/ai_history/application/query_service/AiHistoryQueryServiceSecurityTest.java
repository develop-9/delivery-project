package com.delivery_project.slack_service.ai_history.application.query_service;

import com.delivery_project.slack_service.ai_history.application.query.AiHistorySearchQuery;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistory;
import com.delivery_project.slack_service.ai_history.domain.repository.AiHistoryQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(
        AiHistoryQueryServiceSecurityTest.TestConfig.class
)
@DisplayName("AI History QueryService 권한 테스트")
class AiHistoryQueryServiceSecurityTest {

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(AiHistoryQueryService.class)
    static class TestConfig {
    }

    @Autowired
    private AiHistoryQueryService aiHistoryQueryService;

    @MockitoBean
    private AiHistoryQueryRepository aiHistoryQueryRepository;

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("MASTER는 Service를 직접 호출해 AI 요청 이력을 조회할 수 있다")
    void findById_masterSuccess() {
        // given
        UUID aiHistoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        AiHistory aiHistory = AiHistory.createPending(
                orderId,
                "test prompt",
                Instant.parse("2026-08-09T01:00:00Z")
        );

        when(aiHistoryQueryRepository.findById(aiHistoryId))
                .thenReturn(Optional.of(aiHistory));

        // when
        AiHistoryDetailResult result =
                aiHistoryQueryService.findById(aiHistoryId);

        // then
        assertThat(result.orderId()).isEqualTo(orderId);

        verify(aiHistoryQueryRepository).findById(aiHistoryId);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("MASTER가 아니면 Service를 직접 호출해도 단건 조회할 수 없다")
    void findById_nonMasterForbidden() {
        // given
        UUID aiHistoryId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(
                () -> aiHistoryQueryService.findById(aiHistoryId)
        )
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(aiHistoryQueryRepository);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("MASTER가 아니면 Service를 직접 호출해도 목록 조회할 수 없다")
    void findAll_nonMasterForbidden() {
        // given
        AiHistorySearchQuery query =
                AiHistorySearchQuery.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "requestedAt",
                        "DESC"
                );

        // when & then
        assertThatThrownBy(
                () -> aiHistoryQueryService.findAll(query)
        )
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(aiHistoryQueryRepository);
    }
}