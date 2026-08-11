package com.delivery_project.order_service.global.response;

import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.exception.ErrorDto;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        ErrorDto error,
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
