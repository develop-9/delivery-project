package com.delivery_project.hub_service.global.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code fieldErrors} 는 Validation 실패에만 담긴다.
 * {@code NON_NULL} 이라 나머지 응답에는 필드 자체가 나가지 않는다 — 규약이 "반대쪽 필드는
 * {@code null} 로도 내려보내지 않는다"고 정했기 때문이다.
 */
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
