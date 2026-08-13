package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserDeleteResult;

public record UserDeleteResponse(
		UUID userId,
		Instant deletedAt
) {
	public static UserDeleteResponse from(UserDeleteResult result) {
		return new UserDeleteResponse(result.userId(), result.deletedAt());
	}
}
