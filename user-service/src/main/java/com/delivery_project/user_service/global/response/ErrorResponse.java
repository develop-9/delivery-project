package com.delivery_project.user_service.global.response;

import java.time.Instant;

import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.exception.ErrorDto;

public record ErrorResponse(
		boolean success,
		ErrorDto error,
		Instant timestamp
) {
	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(false, ErrorDto.from(errorCode), Instant.now());
	}
}
