package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.User;

public record UserDeleteResult(
		UUID userId,
		Instant deletedAt
) {
	public static UserDeleteResult from(User user) {
		return new UserDeleteResult(user.getId(), user.getDeletedAt());
	}
}
