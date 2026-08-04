package com.delivery_project.user_service.global.exception;

public record ErrorDto(
		String errorCode,
		String message
) {
	public static ErrorDto from(ErrorCode errorCode) {
		return new ErrorDto(errorCode.name(), errorCode.getMessage());
	}
}
