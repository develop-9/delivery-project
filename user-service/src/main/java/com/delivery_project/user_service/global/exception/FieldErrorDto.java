package com.delivery_project.user_service.global.exception;

import org.springframework.validation.FieldError;

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
