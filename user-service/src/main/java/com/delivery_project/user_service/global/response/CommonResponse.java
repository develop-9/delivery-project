package com.delivery_project.user_service.global.response;

import java.time.Instant;

import com.delivery_project.user_service.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

	private final boolean success;
	private final T data;
	private final ErrorDetail error;
	private final Instant timestamp;

	private CommonResponse(boolean success, T data, ErrorDetail error, Instant timestamp) {
		this.success = success;
		this.data = data;
		this.error = error;
		this.timestamp = timestamp;
	}

	public static <T> CommonResponse<T> success(T data) {
		return new CommonResponse<>(true, data, null, null);
	}

	public static CommonResponse<Void> error(ErrorCode errorCode) {
		return new CommonResponse<>(false, null, new ErrorDetail(errorCode.name(), errorCode.getMessage()), Instant.now());
	}

	@Getter
	public static class ErrorDetail {
		private final String errorCode;
		private final String message;

		private ErrorDetail(String errorCode, String message) {
			this.errorCode = errorCode;
			this.message = message;
		}
	}
}
