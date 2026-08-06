package com.delivery_project.user_service.user.presentation.request;

import com.delivery_project.user_service.user.application.command.UserLoginCommand;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
		@NotBlank
		String username,

		@NotBlank
		String password
) {
	public UserLoginCommand toCommand() {
		return new UserLoginCommand(username, password);
	}
}
