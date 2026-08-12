package com.delivery_project.slack_service.slack.infrastructure.external.slack;

import com.delivery_project.slack_service.slack.application.port.SlackMessageSender;
import com.delivery_project.slack_service.slack.application.result.SlackMessageSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackMessageSenderImpl implements SlackMessageSender {

    private static final Logger log =
            LoggerFactory.getLogger(SlackMessageSenderImpl.class);

    private final RestClient restClient;

    public SlackMessageSenderImpl(
            @Value("${slack.base-url}") String baseUrl,
            @Value("${slack.bot-token}") String botToken
    ) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + botToken
                        )
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .build();
    }

    @Override
    public SlackMessageSendResult send(
            String receiverSlackId,
            String message
    ) {
        try {
            SlackConversationOpenResponse conversationResponse =
                    restClient.post()
                            .uri("/conversations.open")
                            .body(
                                    new SlackConversationOpenRequest(
                                            receiverSlackId
                                    )
                            )
                            .retrieve()
                            .body(
                                    SlackConversationOpenResponse.class
                            );

            if (
                    conversationResponse == null
                            || !conversationResponse.ok()
                            || conversationResponse.channel() == null
                            || conversationResponse.channel().id() == null
            ) {
                String errorMessage =
                        conversationResponse == null
                                ? "Slack conversations.open 응답이 없습니다."
                                : conversationResponse.error();

                log.error(
                        "Slack DM 채널 생성 실패. receiverSlackId={}, error={}",
                        receiverSlackId,
                        errorMessage
                );

                return SlackMessageSendResult.failed(
                        errorMessage
                );
            }

            String dmChannelId =
                    conversationResponse.channel().id();

            SlackPostMessageResponse messageResponse =
                    restClient.post()
                            .uri("/chat.postMessage")
                            .body(
                                    new SlackPostMessageRequest(
                                            dmChannelId,
                                            message
                                    )
                            )
                            .retrieve()
                            .body(
                                    SlackPostMessageResponse.class
                            );

            if (
                    messageResponse == null
                            || !messageResponse.ok()
            ) {
                String errorMessage =
                        messageResponse == null
                                ? "Slack chat.postMessage 응답이 없습니다."
                                : messageResponse.error();

                log.error(
                        "Slack 메시지 발송 실패. receiverSlackId={}, dmChannelId={}, error={}",
                        receiverSlackId,
                        dmChannelId,
                        errorMessage
                );

                return SlackMessageSendResult.failed(
                        errorMessage
                );
            }

            return SlackMessageSendResult.succeeded();

        } catch (Exception exception) {
            log.error(
                    "Slack API 호출 중 예외 발생. receiverSlackId={}",
                    receiverSlackId,
                    exception
            );

            return SlackMessageSendResult.failed(
                    exception.getMessage()
            );
        }
    }

    private record SlackConversationOpenRequest(
            String users
    ) {
    }

    private record SlackConversationOpenResponse(
            boolean ok,
            SlackConversationChannel channel,
            String error
    ) {
    }

    private record SlackConversationChannel(
            String id
    ) {
    }
}