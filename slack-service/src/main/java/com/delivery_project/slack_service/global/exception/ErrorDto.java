package com.delivery_project.slack_service.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorDto(

        @Schema(
                description = "오류 코드",
                example = "INVALID_REQUEST"
        )
        String errorCode,

        @Schema(
                description = "오류 메시지",
                example = "요청 형식이 올바르지 않습니다."
        )
        String message

) {

    public static ErrorDto from(
            ErrorCode errorCode
    ) {
        return new ErrorDto(
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    public static ErrorDto of(
            ErrorCode errorCode,
            String message
    ) {
        return new ErrorDto(
                errorCode.getCode(),
                message
        );
    }
}