package com.delivery_project.slack_service.slack.presentation.request;

import com.delivery_project.slack_service.slack.application.command.SlackInternalMessageCreateCommand;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SlackInternalMessageCreateRequest(

        @NotNull(message = "receiverUserId는 필수입니다.")
        UUID receiverUserId,

        @NotNull(message = "orderId는 필수입니다.")
        UUID orderId,

        @NotNull(message = "finalDispatchDeadline은 필수입니다.")
        Instant finalDispatchDeadline

) {

    public SlackInternalMessageCreateCommand toCommand() {
        return new SlackInternalMessageCreateCommand(
                receiverUserId,
                orderId,
                finalDispatchDeadline
        );
    }
}