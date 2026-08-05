package com.delivery_project.slack_service.domain.slack.controller;

import com.delivery_project.slack_service.domain.slack.dto.request.SlackMessageCreateRequest;
import com.delivery_project.slack_service.domain.slack.dto.request.SlackMessageUpdateRequest;
import com.delivery_project.slack_service.domain.slack.dto.response.SlackMessageResponse;
import com.delivery_project.slack_service.domain.slack.service.SlackMessageService;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Slack Message", description = "Slack 메시지 관리 API")
@RestController
@RequestMapping("/api/v1/slack-messages")
@RequiredArgsConstructor
public class SlackMessageController {

    private static final UUID TEMP_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final SlackMessageService slackMessageService;

    @Operation(summary = "Slack 메시지 생성")
    @PostMapping
    public ResponseEntity<SuccessResponse<SlackMessageResponse>> createSlackMessage(
            @Valid @RequestBody SlackMessageCreateRequest request
    ) {
        SlackMessageResponse response =
                slackMessageService.createSlackMessage(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @Operation(summary = "Slack 메시지 단건 조회")
    @GetMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<SlackMessageResponse>> getSlackMessage(
            @PathVariable UUID slackMessageId
    ) {
        SlackMessageResponse response =
                slackMessageService.getSlackMessage(slackMessageId);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @Operation(summary = "Slack 메시지 목록 조회")
    @GetMapping
    public ResponseEntity<SuccessResponse<List<SlackMessageResponse>>> getSlackMessages() {
        List<SlackMessageResponse> response =
                slackMessageService.getSlackMessages();

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @Operation(summary = "Slack 메시지 수정")
    @PatchMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<SlackMessageResponse>> updateSlackMessage(
            @PathVariable UUID slackMessageId,
            @Valid @RequestBody SlackMessageUpdateRequest request
    ) {
        SlackMessageResponse response =
                slackMessageService.updateSlackMessage(slackMessageId, request);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @Operation(summary = "Slack 메시지 삭제")
    @DeleteMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<Void>> deleteSlackMessage(
            @PathVariable UUID slackMessageId
    ) {
        slackMessageService.deleteSlackMessage(
                slackMessageId,
                TEMP_USER_ID
        );

        return ResponseEntity.ok(SuccessResponse.success(null));
    }
}