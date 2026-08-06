package com.delivery_project.api_gateway.global.response;

import com.delivery_project.api_gateway.global.exception.ErrorCode;

public record ErrorDto(
		String errorCode,
		String message
) {
	public static ErrorDto from(ErrorCode errorCode) {
		return new ErrorDto(errorCode.name(), errorCode.getMessage());
	}
}
