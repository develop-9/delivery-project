package com.delivery_project.user_service.user.application.command;

public record UserLoginCommand(
		String username,
		String password
) {
}
