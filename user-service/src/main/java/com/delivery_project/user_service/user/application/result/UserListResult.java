package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserListResult(
		UUID userId,
		String username,
		String name,
		Role role,
		ApprovalStatus approvalStatus,
		UUID hubId,
		UUID companyId,
		Instant createdAt
) {
	public static UserListResult from(User user) {
		return new UserListResult(
				user.getId(), user.getUsername(), user.getName(), user.getRole(),
				user.getApprovalStatus(), user.getHubId(), user.getCompanyId(), user.getCreatedAt());
	}
}
