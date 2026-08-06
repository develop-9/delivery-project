package com.delivery_project.slack_service.slack.presentation.request;

import com.delivery_project.slack_service.slack.application.command.SlackMessageUpdateCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SlackMessageUpdateRequest(

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String message

) {

        public SlackMessageUpdateCommand toCommand(
                UUID slackMessageId
        ) {
                return new SlackMessageUpdateCommand(
                        slackMessageId,
                        message
                );
        }
}