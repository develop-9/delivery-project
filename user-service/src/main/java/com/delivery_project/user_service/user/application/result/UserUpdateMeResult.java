package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.User;

public record UserUpdateMeResult(
		UUID userId,
		String name,
		String slackId,
		Instant updatedAt
) {
	public static UserUpdateMeResult from(User user) {
		return new UserUpdateMeResult(user.getId(), user.getName(), user.getSlackId(), user.getUpdatedAt());
	}
}
