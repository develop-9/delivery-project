package com.delivery_project.user_service.user.presentation.request;

import com.delivery_project.user_service.user.application.command.UserRefreshCommand;

import jakarta.validation.constraints.NotBlank;

public record UserRefreshRequest(
		@NotBlank
		String refreshToken
) {
	public UserRefreshCommand toCommand() {
		return new UserRefreshCommand(refreshToken);
	}
}
