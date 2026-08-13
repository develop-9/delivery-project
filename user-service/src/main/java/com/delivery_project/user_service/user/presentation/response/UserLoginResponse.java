package com.delivery_project.user_service.user.presentation.response;

import com.delivery_project.user_service.user.application.result.UserLoginResult;

public record UserLoginResponse(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
	public static UserLoginResponse from(UserLoginResult result) {
		return new UserLoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn());
	}
}
