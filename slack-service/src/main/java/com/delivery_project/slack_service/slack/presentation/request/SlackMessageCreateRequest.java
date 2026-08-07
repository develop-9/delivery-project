package com.delivery_project.slack_service.slack.presentation.request;

import com.delivery_project.slack_service.slack.application.command.SlackMessageCreateCommand;
import com.delivery_project.slack_service.slack.domain.entity.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SlackMessageCreateRequest(

        UUID senderUserId,

        @NotNull(message = "발송자 유형은 필수입니다.")
        SenderType senderType,

        @NotNull(message = "수신자 ID는 필수입니다.")
        UUID receiverUserId,

        @NotBlank(message = "수신자의 Slack ID는 필수입니다.")
        @Size(max = 100, message = "Slack ID는 100자를 초과할 수 없습니다.")
        String receiverSlackId,

        @NotBlank(message = "메시지는 필수입니다.")
        String message
) {

        public SlackMessageCreateCommand toCommand() {
                return new SlackMessageCreateCommand(
                        senderUserId,
                        senderType,
                        receiverUserId,
                        receiverSlackId,
                        message
                );
        }
}