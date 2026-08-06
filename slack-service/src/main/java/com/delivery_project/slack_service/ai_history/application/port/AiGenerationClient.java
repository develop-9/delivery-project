package com.delivery_project.slack_service.ai_history.application.port;

import com.delivery_project.slack_service.ai_history.application.result.AiGenerationResult;

public interface AiGenerationClient {

    AiGenerationResult generateFinalDispatchDeadline(
            String prompt
    );
}