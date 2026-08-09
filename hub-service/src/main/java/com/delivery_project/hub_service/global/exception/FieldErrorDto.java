package com.delivery_project.hub_service.global.exception;

import org.springframework.validation.FieldError;

/**
 * Validation 실패 시 어떤 필드가 왜 거절됐는지 알려준다.
 *
 * <p>{@code INVALID_INPUT_VALUE} 만으로는 클라이언트가 어느 입력을 고쳐야 할지 알 수 없다.
 */
public record FieldErrorDto(
		String field,
		String rejectedValue,
		String reason
) {
	public static FieldErrorDto from(FieldError fieldError) {
		Object rejectedValue = fieldError.getRejectedValue();

		return new FieldErrorDto(
				fieldError.getField(),
				rejectedValue != null ? rejectedValue.toString() : null,
				fieldError.getDefaultMessage()
		);
	}
}
