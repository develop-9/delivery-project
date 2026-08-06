package com.delivery_project.slack_service.ai_history.application.result;

import java.time.Instant;

public record AiGenerationResult(
        String modelName,
        Instant finalDispatchDeadline
) {
}