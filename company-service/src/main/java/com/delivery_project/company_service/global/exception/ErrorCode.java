package com.delivery_project.company_service.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // === common ===
    // 400
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.", "EMPTY_FILE"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력조건을 불충족하였습니다.", "INVALID_REQUEST"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다.", "INVALID_INPUT_VALUE"),

    // 401
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다.", "AUTH_UNAUTHORIZED"),

    // 403
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "AUTH_FORBIDDEN"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", "NOT_FOUND"),

    // 409
    INVALID_STATE(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다.", "INVALID_STATE"),

    // 415
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다.", "UNSUPPORTED_MEDIA_TYPE"),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.", "FILE_UPLOAD_FAILED"),


    // === company ===
    // 400
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "올바른 Page 입력이 아닙니다.", "INVALID_PAGE"),

    // 404
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "업체가 존재하지 않습니다.", "COMPANY_NOT_FOUND"),
    ;

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
