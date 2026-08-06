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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Slack Message",
        description = "Slack 메시지 생성, 조회, 수정, 삭제 API"
)
@RestController
@RequestMapping("/api/v1/slack-messages")
@RequiredArgsConstructor
public class SlackMessageApiController {

    private final SlackMessageCommandService slackMessageCommandService;
    private final SlackMessageQueryService slackMessageQueryService;

    @Operation(
            summary = "Slack 메시지 생성",
            description = "Slack 메시지를 PENDING 상태로 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "메시지 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청값"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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
                .body(SuccessResponse.success(response));
    }

    @Operation(
            summary = "Slack 메시지 단건 조회",
            description = "삭제되지 않은 Slack 메시지를 ID로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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

    @Operation(
            summary = "Slack 메시지 목록 조회",
            description = "삭제되지 않은 Slack 메시지를 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<SuccessResponse<PageResponse<SlackMessageQueryResponse>>> findAll(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt"
            ) Pageable pageable
    ) {
        String sortField = pageable.getSort()
                .stream()
                .findFirst()
                .map(order -> order.getProperty())
                .orElse("createdAt");

        String sortDirection = pageable.getSort()
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

    @Operation(
            summary = "Slack 메시지 수정",
            description = "Slack 메시지 내용을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청값"),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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

    @Operation(
            summary = "Slack 메시지 삭제",
            description = "Slack 메시지를 논리 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @ApiResponse(responseCode = "403", description = "삭제 권한이 없음"),
            @ApiResponse(
                    responseCode = "404",
                    description = "메시지가 존재하지 않거나 이미 삭제됨"
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    // TODO: Slack Service JWT 인증 연동 후
    // MASTER/비MASTER/미인증 요청 및 deletedBy 저장값 검증
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