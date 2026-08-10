package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Gemini 실제 API 연동 테스트")
class GeminiAiGenerationClientIntegrationTest {

    @Test
    @DisplayName("실제 Gemini API를 호출하여 최종 발송 시한을 반환한다")
    void generateFinalDispatchDeadline_realApi() {
        // given
        String apiKey =
                System.getenv("GEMINI_API_KEY");

        assumeTrue(
                apiKey != null && !apiKey.isBlank(),
                "GEMINI_API_KEY 환경변수가 없어 실제 API 테스트를 건너뜁니다."
        );

        String baseUrl =
                getEnvOrDefault(
                        "GEMINI_BASE_URL",
                        "https://generativelanguage.googleapis.com"
                );

        String modelName =
                getEnvOrDefault(
                        "GEMINI_MODEL",
                        "gemini-2.5-flash"
                );

        GeminiAiGenerationClient client =
                new GeminiAiGenerationClient(
                        RestClient.builder(),
                        baseUrl,
                        apiKey,
                        modelName
                );

        String prompt = """
                테스트 요청입니다.

                현재 주문의 배송 완료 희망 시각은
                2026-08-11T12:00:00Z 입니다.

                전체 배송 예상 소요 시간은 3시간입니다.

                첫 출발 허브에서 상품을 발송해야 하는
                최종 발송 시한을 계산해 주세요.

                다른 설명은 작성하지 말고
                반드시 ISO-8601 UTC 형식의 시각만 반환해 주세요.

                예:
                2026-08-11T09:00:00Z
                """;

        // when
        AiGenerationResult result =
                client.generateFinalDispatchDeadline(
                        prompt
                );

        // then
        assertThat(result)
                .isNotNull();

        assertThat(result.modelName())
                .isEqualTo(modelName);

        assertThat(result.finalDispatchDeadline())
                .isNotNull();

        assertThat(result.finalDispatchDeadline())
                .isBefore(
                        Instant.parse(
                                "2026-08-11T12:00:00Z"
                        )
                );

        System.out.println(
                "Gemini model = "
                        + result.modelName()
        );

        System.out.println(
                "finalDispatchDeadline = "
                        + result.finalDispatchDeadline()
        );
    }

    private String getEnvOrDefault(
            String key,
            String defaultValue
    ) {
        String value =
                System.getenv(key);

        if (
                value == null
                        || value.isBlank()
        ) {
            return defaultValue;
        }

        return value;
    }
}