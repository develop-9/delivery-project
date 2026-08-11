package com.delivery_project.slack_service.ai_history.presentation.api_controller;

import com.delivery_project.slack_service.ai_history.application.query_service.AiHistoryQueryService;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryListResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.config.JsonAccessDeniedHandler;
import com.delivery_project.slack_service.global.config.JsonAuthenticationEntryPoint;
import com.delivery_project.slack_service.global.config.SecurityConfig;
import com.delivery_project.slack_service.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiHistoryApiController.class)
@Import({
        SecurityConfig.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
@DisplayName("AI History API Controller 테스트")
class AiHistoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiHistoryQueryService aiHistoryQueryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("MASTER 권한으로 AI 요청 이력을 ID로 단건 조회한다")
    void findById_success() throws Exception {
        // given
        UUID aiHistoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        AiHistoryDetailResult result =
                new AiHistoryDetailResult(
                        aiHistoryId,
                        orderId,
                        "gemini-2.5-flash",
                        "test prompt",
                        Instant.parse("2026-08-09T06:00:00Z"),
                        AiHistoryStatus.SUCCESS,
                        Instant.parse("2026-08-09T01:00:00Z"),
                        Instant.parse("2026-08-09T01:00:05Z"),
                        Instant.parse("2026-08-09T01:00:00Z"),
                        UUID.randomUUID()
                );

        when(aiHistoryQueryService.findById(aiHistoryId))
                .thenReturn(result);

        // when & then
        mockMvc.perform(
                        get(
                                "/api/v1/ai-histories/{aiHistoryId}",
                                aiHistoryId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiHistoryId")
                        .value(aiHistoryId.toString()))
                .andExpect(jsonPath("$.data.orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.data.modelName")
                        .value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.data.status")
                        .value("SUCCESS"));

        verify(aiHistoryQueryService).findById(aiHistoryId);
    }

    @Test
    @WithMockUser(roles = "MASTER")
    @DisplayName("MASTER 권한으로 검색 조건에 맞는 AI 요청 이력 목록을 조회한다")
    void findAll_success() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();

        AiHistoryListResult history =
                new AiHistoryListResult(
                        UUID.randomUUID(),
                        orderId,
                        "gemini-2.5-flash",
                        Instant.parse("2026-08-09T06:00:00Z"),
                        AiHistoryStatus.SUCCESS,
                        Instant.parse("2026-08-09T01:00:00Z"),
                        Instant.parse("2026-08-09T01:00:05Z")
                );

        PageData<AiHistoryListResult> result =
                new PageData<>(
                        List.of(history),
                        0,
                        10,
                        1L,
                        1
                );

        when(aiHistoryQueryService.findAll(any()))
                .thenReturn(result);

        // when & then
        mockMvc.perform(
                        get("/api/v1/ai-histories")
                                .param(
                                        "orderId",
                                        orderId.toString()
                                )
                                .param("status", "SUCCESS")
                                .param(
                                        "modelName",
                                        "gemini-2.5-flash"
                                )
                                .param("page", "0")
                                .param("size", "10")
                                .param(
                                        "sort",
                                        "requestedAt,desc"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("SUCCESS"));

        verify(aiHistoryQueryService).findAll(any());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 AI 요청 이력을 조회할 수 없다")
    void findById_unauthenticated() throws Exception {
        // given
        UUID aiHistoryId = UUID.randomUUID();

        // when & then
        mockMvc.perform(
                        get(
                                "/api/v1/ai-histories/{aiHistoryId}",
                                aiHistoryId
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode")
                        .value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(aiHistoryQueryService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("MASTER가 아닌 사용자는 AI 요청 이력을 조회할 수 없다")
    void findById_forbidden() throws Exception {
        // given
        UUID aiHistoryId = UUID.randomUUID();

        // when & then
        mockMvc.perform(
                        get(
                                "/api/v1/ai-histories/{aiHistoryId}",
                                aiHistoryId
                        )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode")
                        .value("AUTH_FORBIDDEN"));

        verifyNoInteractions(aiHistoryQueryService);
    }
}