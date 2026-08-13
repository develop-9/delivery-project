package com.delivery_project.slack_service.slack.presentation.api_controller;

import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.response.PageResponse;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import com.delivery_project.slack_service.slack.application.command.SlackMessageDeleteCommand;
import com.delivery_project.slack_service.slack.application.command_service.SlackMessageCommandService;
import com.delivery_project.slack_service.slack.application.query_service.SlackMessageQueryService;
import com.delivery_project.slack_service.slack.application.result.SlackMessageCreateResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageDeleteResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageQueryResult;
import com.delivery_project.slack_service.slack.application.result.SlackMessageUpdateResult;
import com.delivery_project.slack_service.slack.presentation.request.SlackMessageCreateRequest;
import com.delivery_project.slack_service.slack.presentation.request.SlackMessageUpdateRequest;
import com.delivery_project.slack_service.slack.presentation.response.SlackMessageCreateResponse;
import com.delivery_project.slack_service.slack.presentation.response.SlackMessageDeleteResponse;
import com.delivery_project.slack_service.slack.presentation.response.SlackMessageQueryResponse;
import com.delivery_project.slack_service.slack.presentation.response.SlackMessageUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slack-messages")
@RequiredArgsConstructor
public class SlackMessageApiController implements SlackMessageApi {

    private final SlackMessageCommandService slackMessageCommandService;
    private final SlackMessageQueryService slackMessageQueryService;

    @Override
    @PostMapping
    public ResponseEntity<SuccessResponse<SlackMessageCreateResponse>> create(
            @Valid @RequestBody SlackMessageCreateRequest request
    ) {
        SlackMessageCreateResult result =
                slackMessageCommandService.create(
                        request.toCommand()
                );

        SlackMessageCreateResponse response =
                SlackMessageCreateResponse.from(result);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        SuccessResponse.success(response)
                );
    }

    @Override
    @GetMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<SlackMessageQueryResponse>> findById(
            @PathVariable UUID slackMessageId
    ) {
        SlackMessageQueryResult result =
                slackMessageQueryService.findById(
                        slackMessageId
                );

        SlackMessageQueryResponse response =
                SlackMessageQueryResponse.from(result);

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<SuccessResponse<PageResponse<SlackMessageQueryResponse>>> findAll(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt"
            )
            Pageable pageable
    ) {
        String sortField =
                pageable.getSort()
                        .stream()
                        .findFirst()
                        .map(order -> order.getProperty())
                        .orElse("createdAt");

        String sortDirection =
                pageable.getSort()
                        .stream()
                        .findFirst()
                        .map(order -> order.getDirection().name())
                        .orElse("DESC");

        PageData<SlackMessageQueryResult> result =
                slackMessageQueryService.findAll(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        sortField,
                        sortDirection
                );

        PageResponse<SlackMessageQueryResponse> response =
                PageResponse.from(
                        result,
                        SlackMessageQueryResponse::from
                );

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }

    @Override
    @PatchMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<SlackMessageUpdateResponse>> update(
            @PathVariable UUID slackMessageId,
            @Valid @RequestBody SlackMessageUpdateRequest request
    ) {
        SlackMessageUpdateResult result =
                slackMessageCommandService.update(
                        request.toCommand(slackMessageId)
                );

        SlackMessageUpdateResponse response =
                SlackMessageUpdateResponse.from(result);

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }

    // TODO: Slack Service JWT 인증 연동 후
    // MASTER/비MASTER/미인증 요청 및 deletedBy 저장값 검증
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{slackMessageId}")
    public ResponseEntity<SuccessResponse<SlackMessageDeleteResponse>> delete(
            @PathVariable UUID slackMessageId,
            @AuthenticationPrincipal UUID callerId
    ) {
        SlackMessageDeleteCommand command =
                SlackMessageDeleteCommand.of(
                        slackMessageId,
                        callerId
                );

        SlackMessageDeleteResult result =
                slackMessageCommandService.delete(command);

        SlackMessageDeleteResponse response =
                SlackMessageDeleteResponse.from(result);

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }
}