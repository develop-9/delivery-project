package com.delivery_project.slack_service.global.exception;

public record ErrorDto(
        String errorCode,
        String message
) {

    public static ErrorDto from(ErrorCode errorCode) {
        return new ErrorDto(
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}