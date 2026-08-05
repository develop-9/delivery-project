package com.delivery_project.slack_service.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다.",
            "INTERNAL_SERVER_ERROR"
    ),

    NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "리소스를 찾을 수 없습니다.",
            "NOT_FOUND"
    ),

    INVALID_STATE(
            HttpStatus.CONFLICT,
            "요청을 처리할 수 없는 상태입니다.",
            "INVALID_STATE"
    ),

    INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "요청값이 올바르지 않습니다.",
            "INVALID_INPUT_VALUE"
    );

    private final HttpStatus status;
    private final String message;
    private final String code;

    ErrorCode(
            HttpStatus status,
            String message,
            String code
    ) {
        this.status = status;
        this.message = message;
        this.code = code;
    }
}