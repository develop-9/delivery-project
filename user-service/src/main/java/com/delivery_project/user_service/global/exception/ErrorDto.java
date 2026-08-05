package com.delivery_project.user_service.global.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDto(
		String errorCode,
		String message,
		List<FieldErrorDto> fieldErrors
) {
	public static ErrorDto from(ErrorCode errorCode) {
		return new ErrorDto(errorCode.name(), errorCode.getMessage(), null);
	}

	public static ErrorDto from(ErrorCode errorCode, List<FieldErrorDto> fieldErrors) {
		return new ErrorDto(errorCode.name(), errorCode.getMessage(), fieldErrors);
	}
}
