package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Gemini AI Generation Client 테스트")
class GeminiAiGenerationClientTest {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com";

    private static final String API_KEY =
            "test-api-key";

    private static final String MODEL_NAME =
            "gemini-2.5-flash";

    @Test
    @DisplayName("Gemini가 ISO-8601 시각을 반환하면 최종 발송 시한으로 변환한다")
    void generateFinalDispatchDeadline_success() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        String responseBody =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "2026-08-10T12:00:00Z"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andExpect(
                        header(
                                "x-goog-api-key",
                                API_KEY
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        // when
        AiGenerationResult result =
                client.generateFinalDispatchDeadline(
                        "테스트 프롬프트"
                );

        // then
        assertThat(result.modelName())
                .isEqualTo(MODEL_NAME);

        assertThat(result.finalDispatchDeadline())
                .isEqualTo(
                        Instant.parse(
                                "2026-08-10T12:00:00Z"
                        )
                );

        server.verify();
    }

    @Test
    @DisplayName("Gemini 응답 텍스트가 여러 Part로 나뉘어도 시각을 추출한다")
    void generateFinalDispatchDeadline_multipleParts() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        String responseBody =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "최종 발송 시한은 "
                          },
                          {
                            "text": "2026-08-10T12:00:00Z"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        // when
        AiGenerationResult result =
                client.generateFinalDispatchDeadline(
                        "테스트 프롬프트"
                );

        // then
        assertThat(result.finalDispatchDeadline())
                .isEqualTo(
                        Instant.parse(
                                "2026-08-10T12:00:00Z"
                        )
                );

        server.verify();
    }

    @Test
    @DisplayName("Gemini 응답에 candidates가 없으면 파싱 예외가 발생한다")
    void generateFinalDispatchDeadline_candidatesEmpty() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        String responseBody =
                """
                {
                  "candidates": []
                }
                """;

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        // when & then
        assertThatThrownBy(
                () ->
                        client.generateFinalDispatchDeadline(
                                "테스트 프롬프트"
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                );

        server.verify();
    }

    @Test
    @DisplayName("Gemini 응답에 parts가 없으면 파싱 예외가 발생한다")
    void generateFinalDispatchDeadline_partsEmpty() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        String responseBody =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": []
                      }
                    }
                  ]
                }
                """;

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        // when & then
        assertThatThrownBy(
                () ->
                        client.generateFinalDispatchDeadline(
                                "테스트 프롬프트"
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                );

        server.verify();
    }

    @Test
    @DisplayName("Gemini 응답에 ISO-8601 시각이 없으면 파싱 예외가 발생한다")
    void generateFinalDispatchDeadline_invalidResponseText() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        String responseBody =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "최종 발송 시한을 계산할 수 없습니다."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        // when & then
        assertThatThrownBy(
                () ->
                        client.generateFinalDispatchDeadline(
                                "테스트 프롬프트"
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                );

        server.verify();
    }

    @Test
    @DisplayName("API Key가 없으면 Gemini API를 호출하지 않고 예외가 발생한다")
    void generateFinalDispatchDeadline_apiKeyMissing() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        "",
                        MODEL_NAME
                );

        // when & then
        assertThatThrownBy(
                () ->
                        client.generateFinalDispatchDeadline(
                                "테스트 프롬프트"
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                );

        // HTTP 요청 자체가 발생하지 않아야 함
        server.verify();
    }

    @Test
    @DisplayName("Gemini API 호출 자체가 실패하면 예외가 발생한다")
    void generateFinalDispatchDeadline_apiRequestFailure() {
        // given
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        builder,
                        BASE_URL,
                        API_KEY,
                        MODEL_NAME
                );

        server.expect(
                        once(),
                        requestTo(
                                BASE_URL
                                        + "/v1beta/models/"
                                        + MODEL_NAME
                                        + ":generateContent"
                        )
                )
                .andRespond(
                        withServerError()
                );

        // when & then
        assertThatThrownBy(
                () ->
                        client.generateFinalDispatchDeadline(
                                "테스트 프롬프트"
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                );

        server.verify();
    }
}