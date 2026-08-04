package com.delivery_project.user_service.user.application.command;

public record UserRefreshCommand(
		String refreshToken
) {
}
