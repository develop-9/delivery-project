package com.delivery_project.slack_service.ai_history.presentation.api_controller;

import com.delivery_project.slack_service.ai_history.application.command.AiHistorySearchCommand;
import com.delivery_project.slack_service.ai_history.application.query_service.AiHistoryQueryService;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryDetailResult;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryListResult;
import com.delivery_project.slack_service.ai_history.domain.entity.AiHistoryStatus;
import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryDetailResponse;
import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryListResponse;
import com.delivery_project.slack_service.global.common.PageData;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.global.response.PageResponse;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Tag(
        name = "AI History",
        description = "AI 요청 이력 단건 및 목록 조회 API"
)
@RestController
@RequestMapping("/api/v1/ai-histories")
@RequiredArgsConstructor
public class AiHistoryApiController {

    private final AiHistoryQueryService aiHistoryQueryService;

    @Operation(
            summary = "AI 요청 이력 단건 조회",
            description = "AI 요청 이력 ID로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 요청 이력 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "조회 권한 없음"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "AI 요청 이력을 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/{aiHistoryId}")
    public ResponseEntity<SuccessResponse<AiHistoryDetailResponse>> findById(
            @PathVariable UUID aiHistoryId
    ) {
        AiHistoryDetailResult result =
                aiHistoryQueryService.findById(aiHistoryId);

        AiHistoryDetailResponse response =
                AiHistoryDetailResponse.from(result);

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }

    @Operation(
            summary = "AI 요청 이력 목록 조회",
            description = "검색 조건을 이용해 AI 요청 이력을 페이지 형태로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 요청 이력 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 검색 조건"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "조회 권한 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping
    public ResponseEntity<SuccessResponse<PageResponse<AiHistoryListResponse>>> findAll(
            @RequestParam(required = false)
            UUID orderId,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            String modelName,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "requestedAt,desc")
            String sort
    ) {
        String[] sortValues = parseSort(sort);

        AiHistorySearchCommand command =
                AiHistorySearchCommand.of(
                        orderId,
                        parseStatus(status),
                        modelName,
                        startDate,
                        endDate,
                        page,
                        size,
                        sortValues[0],
                        sortValues[1]
                );

        PageData<AiHistoryListResult> result =
                aiHistoryQueryService.findAll(command);

        PageResponse<AiHistoryListResponse> response =
                PageResponse.from(
                        result,
                        AiHistoryListResponse::from
                );

        return ResponseEntity.ok(
                SuccessResponse.success(response)
        );
    }

    private AiHistoryStatus parseStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return AiHistoryStatus.valueOf(
                    status.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }
    }

    private String[] parseSort(
            String sort
    ) {
        if (sort == null || sort.isBlank()) {
            return new String[]{
                    "requestedAt",
                    "desc"
            };
        }

        String[] sortValues = sort.split(",", -1);

        if (sortValues.length != 2) {
            throw new BusinessException(
                    ErrorCode.INVALID_AI_HISTORY_SEARCH_CONDITION
            );
        }

        return new String[]{
                sortValues[0].trim(),
                sortValues[1].trim()
        };
    }
}