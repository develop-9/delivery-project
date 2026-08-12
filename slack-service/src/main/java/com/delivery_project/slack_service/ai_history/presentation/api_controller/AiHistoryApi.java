package com.delivery_project.slack_service.ai_history.presentation.api_controller;

import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryDetailResponse;
import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryListResponse;
import com.delivery_project.slack_service.global.response.ErrorResponse;
import com.delivery_project.slack_service.global.response.PageResponse;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

@Tag(
        name = "AI History",
        description = "AI 요청 이력 단건 및 목록 조회 API"
)
public interface AiHistoryApi {

    @Operation(
            summary = "AI 요청 이력 단건 조회",
            description = "AI 요청 이력 ID로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 요청 이력 단건 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "AI 요청 이력을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    ResponseEntity<SuccessResponse<AiHistoryDetailResponse>> findById(
            @PathVariable UUID aiHistoryId
    );

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
                    description = "유효하지 않은 검색 조건",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    ResponseEntity<SuccessResponse<PageResponse<AiHistoryListResponse>>> findAll(
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
    );
}