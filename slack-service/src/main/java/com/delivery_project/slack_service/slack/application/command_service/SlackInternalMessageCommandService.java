package com.delivery_project.slack_service.slack.application.command_service;

import com.delivery_project.slack_service.slack.application.command.SlackInternalMessageCreateCommand;
import com.delivery_project.slack_service.slack.application.port.SlackMessageQueuePublisher;
import com.delivery_project.slack_service.slack.application.port.UserSlackIdClient;
import com.delivery_project.slack_service.slack.application.result.UserSlackIdResult;
import com.delivery_project.slack_service.slack.domain.entity.SlackMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlackInternalMessageCommandService {

    private final UserSlackIdClient userSlackIdClient;
    private final SlackMessageCommandService slackMessageCommandService;
    private final SlackMessageQueuePublisher slackMessageQueuePublisher;

    public void createAndPublish(
            SlackInternalMessageCreateCommand command
    ) {
        UserSlackIdResult receiver =
                userSlackIdClient.getUserSlackId(
                        command.receiverUserId()
                );

        String message =
                createMessage(command);

        SlackMessage slackMessage =
                slackMessageCommandService.createPending(
                        command.receiverUserId(),
                        receiver.slackId(),
                        message
                );

        slackMessageQueuePublisher.publish(
                slackMessage.getId(),
                receiver.slackId(),
                message
        );
    }

    private String createMessage(
            SlackInternalMessageCreateCommand command
    ) {
        return """
                [배송 알림]
                주문 ID: %s
                최종 발송 시한: %s

                해당 주문이 배송 예정 시간 내 완료될 수 있도록
                위 시각까지 출발 허브에서 상품 발송을 진행해 주세요.
                """.formatted(
                command.orderId(),
                command.finalDispatchDeadline()
        );
    }
}