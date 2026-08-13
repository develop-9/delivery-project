package com.delivery_project.slack_service.global.response;

import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.global.exception.ErrorDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ErrorResponse(

        @Schema(
                description = "요청 처리 성공 여부",
                example = "false"
        )
        boolean success,

        @Schema(
                description = "오류 정보"
        )
        ErrorDto error,

        @Schema(
                description = "응답 발생 시각",
                example = "2026-08-07T00:18:00Z"
        )
        Instant timestamp

) {

    public static ErrorResponse from(
            ErrorCode errorCode
    ) {
        return new ErrorResponse(
                false,
                ErrorDto.from(errorCode),
                Instant.now()
        );
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String message
    ) {
        return new ErrorResponse(
                false,
                ErrorDto.of(errorCode, message),
                Instant.now()
        );
    }
}