package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.port.AiGenerationClient;
import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiAiGenerationClient
        implements AiGenerationClient {

    private static final Pattern INSTANT_PATTERN =
            Pattern.compile(
                    "\\d{4}-\\d{2}-\\d{2}T"
                            + "\\d{2}:\\d{2}:\\d{2}"
                            + "(?:\\.\\d+)?Z"
            );

    private final RestClient restClient;
    private final String apiKey;
    private final String modelName;

    public GeminiAiGenerationClient(
            RestClient.Builder restClientBuilder,
            @Value(
                    "${gemini.base-url:"
                            + "https://generativelanguage.googleapis.com}"
            )
            String baseUrl,
            @Value("${gemini.api-key:}")
            String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}")
            String modelName
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public AiGenerationResult generateFinalDispatchDeadline(
            String prompt
    ) {
        validateApiKey();

        GeminiRequest request =
                new GeminiRequest(
                        List.of(
                                new Content(
                                        List.of(
                                                new Part(prompt)
                                        )
                                )
                        )
                );

        GeminiResponse response;

        try {
            response = restClient.post()
                    .uri(
                            "/v1beta/models/{model}:generateContent",
                            modelName
                    )
                    .header(
                            "x-goog-api-key",
                            apiKey
                    )
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_REQUEST_FAILED
            );
        }

        String responseText =
                extractResponseText(response);

        Instant finalDispatchDeadline =
                parseFinalDispatchDeadline(responseText);

        return new AiGenerationResult(
                modelName,
                finalDispatchDeadline
        );
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_REQUEST_FAILED
            );
        }
    }

    private String extractResponseText(
            GeminiResponse response
    ) {
        if (
                response == null
                        || response.candidates() == null
                        || response.candidates().isEmpty()
        ) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED
            );
        }

        Candidate candidate =
                response.candidates().getFirst();

        if (
                candidate.content() == null
                        || candidate.content().parts() == null
                        || candidate.content().parts().isEmpty()
        ) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED
            );
        }

        String text =
                candidate.content()
                        .parts()
                        .stream()
                        .map(ResponsePart::text)
                        .filter(value ->
                                value != null
                                        && !value.isBlank()
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.AI_RESPONSE_PARSE_FAILED
                                )
                        );

        return text.trim();
    }

    private Instant parseFinalDispatchDeadline(
            String responseText
    ) {
        Matcher matcher =
                INSTANT_PATTERN.matcher(responseText);

        if (!matcher.find()) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED
            );
        }

        try {
            return Instant.parse(
                    matcher.group()
            );
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED
            );
        }
    }

    private record GeminiRequest(
            List<Content> contents
    ) {
    }

    private record Content(
            List<Part> parts
    ) {
    }

    private record Part(
            String text
    ) {
    }

    private record GeminiResponse(
            List<Candidate> candidates
    ) {
    }

    private record Candidate(
            ResponseContent content
    ) {
    }

    private record ResponseContent(
            List<ResponsePart> parts
    ) {
    }

    private record ResponsePart(
            String text
    ) {
    }
}