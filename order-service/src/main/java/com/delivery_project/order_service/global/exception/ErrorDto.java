package com.delivery_project.order_service.global.exception;

public record ErrorDto(
        String errorCode,
        String message
) {
    public static ErrorDto from(ErrorCode errorCode) {
        return new ErrorDto(errorCode.getCode(), errorCode.getMessage());
    }

    /** 예외가 상세 메시지를 들고 있을 때는 그것을 우선한다 */
    public static ErrorDto of(ErrorCode errorCode, String message) {
        return new ErrorDto(errorCode.getCode(),
                message != null && !message.isBlank() ? message : errorCode.getMessage());
    }
}
