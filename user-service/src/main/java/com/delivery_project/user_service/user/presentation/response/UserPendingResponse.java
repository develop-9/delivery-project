package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.Role;

public record UserPendingResponse(
		UUID userId,
		String username,
		String name,
		Role role,
		UUID hubId,
		Instant createdAt
) {
	public static UserPendingResponse from(UserPendingResult result) {
		return new UserPendingResponse(
				result.userId(), result.username(), result.name(), result.role(), result.hubId(), result.createdAt());
	}
}
