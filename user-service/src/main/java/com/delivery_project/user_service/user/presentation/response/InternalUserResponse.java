package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.InternalUserResult;
import com.delivery_project.user_service.user.domain.entity.Role;

public record InternalUserResponse(
		UUID userId,
		String username,
		String name,
		Role role,
		UUID hubId,
		UUID companyId
) {
	public static InternalUserResponse from(InternalUserResult result) {
		return new InternalUserResponse(
				result.userId(), result.username(), result.name(), result.role(), result.hubId(), result.companyId());
	}
}
