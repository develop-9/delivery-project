package com.delivery_project.slack_service.slack.presentation.request;

import com.delivery_project.slack_service.slack.application.command.SlackMessageUpdateCommand;
import jakarta.validation.constraints.NotBlank;

public record SlackMessageUpdateRequest(

        @NotBlank(message = "메시지는 필수입니다.")
        String message
) {

        public SlackMessageUpdateCommand toCommand() {
                return new SlackMessageUpdateCommand(message);
        }
}