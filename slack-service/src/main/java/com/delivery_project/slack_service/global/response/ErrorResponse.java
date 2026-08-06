package com.delivery_project.slack_service.global.response;

import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.global.exception.ErrorDto;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        ErrorDto error,
        Instant timestamp
) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                false,
                ErrorDto.from(errorCode),
                Instant.now()
        );
    }
}