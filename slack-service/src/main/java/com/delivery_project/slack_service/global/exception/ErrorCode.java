package com.delivery_project.slack_service.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400
    INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "요청값이 올바르지 않습니다.",
            "INVALID_INPUT_VALUE"
    ),

    SLACK_MESSAGE_SENDER_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "USER 발송 시 senderUserId는 필수입니다.",
            "SLACK_MESSAGE_SENDER_REQUIRED"
    ),

    INVALID_AI_HISTORY_SEARCH_CONDITION(
            HttpStatus.BAD_REQUEST,
            "유효하지 않은 AI 요청 이력 검색 조건입니다.",
            "INVALID_AI_HISTORY_SEARCH_CONDITION"
    ),

    // 404
    NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "요청한 리소스를 찾을 수 없습니다.",
            "NOT_FOUND"
    ),

    SLACK_MESSAGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Slack 메시지가 존재하지 않습니다.",
            "SLACK_MESSAGE_NOT_FOUND"
    ),

    AI_HISTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI 요청 이력이 존재하지 않습니다.",
            "AI_HISTORY_NOT_FOUND"
    ),

    // 409
    INVALID_STATE(
            HttpStatus.CONFLICT,
            "요청을 처리할 수 없는 상태입니다.",
            "INVALID_STATE"
    ),

    // 500
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다.",
            "INTERNAL_SERVER_ERROR"
    ),

    DEPENDENCY_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "연관 서비스를 사용할 수 없습니다.",
            "DEPENDENCY_SERVICE_UNAVAILABLE"
    ),
    AI_REQUEST_FAILED(
            HttpStatus.BAD_GATEWAY,
            "AI 요청에 실패했습니다.",
            "AI_REQUEST_FAILED"
    ),

    AI_RESPONSE_PARSE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "AI 응답 분석에 실패했습니다.",
            "AI_RESPONSE_PARSE_FAILED"
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