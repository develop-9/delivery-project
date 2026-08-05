package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserPendingResult(
		UUID userId,
		String username,
		String name,
		Role role,
		UUID hubId,
		Instant createdAt
) {
	public static UserPendingResult from(User user) {
		return new UserPendingResult(
				user.getId(), user.getUsername(), user.getName(), user.getRole(), user.getHubId(), user.getCreatedAt());
	}
}
