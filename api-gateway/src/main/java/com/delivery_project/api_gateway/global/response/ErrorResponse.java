package com.delivery_project.api_gateway.global.response;

import java.time.Instant;

import com.delivery_project.api_gateway.global.exception.ErrorCode;

public record ErrorResponse(
		boolean success,
		ErrorDto error,
		Instant timestamp
) {
	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(false, ErrorDto.from(errorCode), Instant.now());
	}
}
