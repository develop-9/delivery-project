package com.delivery_project.user_service.user.presentation.response;

import com.delivery_project.user_service.user.application.result.UserRefreshResult;

public record UserRefreshResponse(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
	public static UserRefreshResponse from(UserRefreshResult result) {
		return new UserRefreshResponse(result.accessToken(), result.refreshToken(), result.expiresIn());
	}
}
