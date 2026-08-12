package com.delivery_project.slack_service.ai_history.presentation.internal_controller;

import com.delivery_project.slack_service.ai_history.application.command_service.AiHistoryCommandService;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiHistoryInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AI History Internal Controller 테스트")
class AiHistoryInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiHistoryCommandService aiHistoryCommandService;

    @Test
    @DisplayName("AI 요청 이력 생성에 성공하면 201 Created를 반환한다")
    void create_success() throws Exception {
        // given
        UUID aiHistoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        AiHistoryCreateResult result =
                new AiHistoryCreateResult(
                        aiHistoryId,
                        orderId,
                        "gemini-2.5-flash",
                        Instant.parse("2026-08-09T06:00:00Z"),
                        AiHistoryStatus.SUCCESS,
                        Instant.parse("2026-08-09T01:00:00Z"),
                        Instant.parse("2026-08-09T01:00:05Z")
                );

        when(aiHistoryCommandService.create(any()))
                .thenReturn(result);

        String requestBody = """
                {
                  "orderId": "%s"
                }
                """.formatted(orderId);

        // when & then
        mockMvc.perform(
                        post("/internal/v1/ai-histories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiHistoryId")
                        .value(aiHistoryId.toString()))
                .andExpect(jsonPath("$.data.orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.data.modelName")
                        .value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.data.status")
                        .value("SUCCESS"));

        verify(aiHistoryCommandService).create(any());
    }

    @Test
    @DisplayName("orderId가 없으면 400 Bad Request를 반환한다")
    void create_orderIdMissing() throws Exception {
        // given
        String requestBody = """
                {
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/internal/v1/ai-histories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(
                aiHistoryCommandService
        );
    }
}