package com.delivery_project.slack_service.ai_history.presentation.request;

import com.delivery_project.slack_service.ai_history.application.command.AiHistoryCreateCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AiHistoryCreateRequest(

        @NotNull(message = "orderId는 필수입니다.")
        UUID orderId

) {

    public AiHistoryCreateCommand toCommand() {
        return new AiHistoryCreateCommand(
                orderId
        );
    }
}