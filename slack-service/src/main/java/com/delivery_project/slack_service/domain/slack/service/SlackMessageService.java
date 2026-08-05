package com.delivery_project.slack_service.domain.slack.service;

import com.delivery_project.slack_service.domain.slack.dto.request.SlackMessageCreateRequest;
import com.delivery_project.slack_service.domain.slack.dto.request.SlackMessageUpdateRequest;
import com.delivery_project.slack_service.domain.slack.dto.response.SlackMessageResponse;
import com.delivery_project.slack_service.domain.slack.entity.SlackMessage;
import com.delivery_project.slack_service.domain.slack.repository.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageService {

    private final SlackMessageRepository slackMessageRepository;

    @Transactional
    public SlackMessageResponse createSlackMessage(
            SlackMessageCreateRequest request
    ) {
        SlackMessage slackMessage = SlackMessage.create(
                request.senderUserId(),
                request.senderType(),
                request.receiverUserId(),
                request.receiverSlackId(),
                request.message()
        );

        SlackMessage savedSlackMessage =
                slackMessageRepository.save(slackMessage);

        return SlackMessageResponse.from(savedSlackMessage);
    }

    public SlackMessageResponse getSlackMessage(UUID slackMessageId) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        return SlackMessageResponse.from(slackMessage);
    }

    public List<SlackMessageResponse> getSlackMessages() {
        return slackMessageRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(SlackMessageResponse::from)
                .toList();
    }

    @Transactional
    public SlackMessageResponse updateSlackMessage(
            UUID slackMessageId,
            SlackMessageUpdateRequest request
    ) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        slackMessage.updateMessage(request.message());

        return SlackMessageResponse.from(slackMessage);
    }

    @Transactional
    public void deleteSlackMessage(
            UUID slackMessageId,
            UUID deletedBy
    ) {
        SlackMessage slackMessage = findSlackMessage(slackMessageId);

        slackMessage.delete(deletedBy);
    }

    private SlackMessage findSlackMessage(UUID slackMessageId) {
        return slackMessageRepository
                .findByIdAndDeletedAtIsNull(slackMessageId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Slack 메시지를 찾을 수 없습니다."
                        )
                );
    }
}