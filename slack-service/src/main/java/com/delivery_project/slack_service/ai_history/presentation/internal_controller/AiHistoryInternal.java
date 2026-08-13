package com.delivery_project.slack_service.ai_history.presentation.internal_controller;

import com.delivery_project.slack_service.ai_history.presentation.request.AiHistoryCreateRequest;
import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryCreateResponse;
import com.delivery_project.slack_service.global.response.ErrorResponse;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(
        name = "AI History Internal",
        description = "AI 최종 발송 시한 생성 내부 API"
)
public interface AiHistoryInternal {

    @Operation(
            summary = "AI 최종 발송 시한 생성",
            description =
                    "주문 및 배송 경로 정보를 조회하고 "
                            + "AI를 이용하여 최종 발송 시한을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "최종 발송 시한 생성 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "주문 또는 배송 경로를 찾을 수 없음",
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
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 요청 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "연관 서비스 사용 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    ResponseEntity<SuccessResponse<AiHistoryCreateResponse>> create(
            @Valid @RequestBody
            AiHistoryCreateRequest request
    );
}