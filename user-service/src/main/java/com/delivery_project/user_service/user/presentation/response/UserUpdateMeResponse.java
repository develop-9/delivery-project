package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;

public record UserUpdateMeResponse(
		UUID userId,
		String name,
		String slackId,
		Instant updatedAt
) {
	public static UserUpdateMeResponse from(UserUpdateMeResult result) {
		return new UserUpdateMeResponse(result.userId(), result.name(), result.slackId(), result.updatedAt());
	}
}
