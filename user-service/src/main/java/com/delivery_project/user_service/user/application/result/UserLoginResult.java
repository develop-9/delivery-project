package com.delivery_project.user_service.user.application.result;

public record UserLoginResult(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
}
