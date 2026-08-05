package com.delivery_project.user_service.user.application.result;

public record UserRefreshResult(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
}
