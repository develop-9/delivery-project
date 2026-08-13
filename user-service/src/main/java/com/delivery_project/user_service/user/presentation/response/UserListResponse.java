package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserListResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;

public record UserListResponse(
		UUID userId,
		String username,
		String name,
		Role role,
		ApprovalStatus approvalStatus,
		UUID hubId,
		UUID companyId,
		Instant createdAt
) {
	public static UserListResponse from(UserListResult result) {
		return new UserListResponse(
				result.userId(), result.username(), result.name(), result.role(),
				result.approvalStatus(), result.hubId(), result.companyId(), result.createdAt());
	}
}
