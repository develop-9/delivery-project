package com.delivery_project.slack_service.ai_history.infrastructure.adapter;

import com.delivery_project.slack_service.ai_history.application.port.AiGeneratePort;
import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;
import com.delivery_project.slack_service.ai_history.infrastructure.client.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiGenerateAdapter implements AiGeneratePort {

    private final GeminiClient geminiClient;

    @Override
    public AiGenerationResult generateFinalDispatchDeadline(
            String prompt
    ) {
        GeminiClient.GeminiGenerationResponse response =
                geminiClient.generateFinalDispatchDeadline(
                        prompt
                );

        return new AiGenerationResult(
                response.modelName(),
                response.finalDispatchDeadline()
        );
    }
}