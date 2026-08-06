package com.delivery_project.user_service.global.response;

import java.time.Instant;
import java.util.List;

import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.exception.ErrorDto;
import com.delivery_project.user_service.global.exception.FieldErrorDto;

public record ErrorResponse(
		boolean success,
		ErrorDto error,
		Instant timestamp
) {
	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(false, ErrorDto.from(errorCode), Instant.now());
	}

	public static ErrorResponse from(ErrorCode errorCode, List<FieldErrorDto> fieldErrors) {
		return new ErrorResponse(false, ErrorDto.from(errorCode, fieldErrors), Instant.now());
	}

	public static ErrorResponse from(ErrorCode errorCode, String message) {
		return new ErrorResponse(false, ErrorDto.from(errorCode, message), Instant.now());
	}
}
