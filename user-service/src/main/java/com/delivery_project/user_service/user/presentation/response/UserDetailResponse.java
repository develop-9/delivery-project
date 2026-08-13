package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;

public record UserDetailResponse(
		UUID userId,
		String username,
		String name,
		String slackId,
		Role role,
		ApprovalStatus approvalStatus,
		UUID hubId,
		UUID companyId,
		Instant createdAt
) {
	public static UserDetailResponse from(UserDetailResult result) {
		return new UserDetailResponse(
				result.userId(), result.username(), result.name(), result.slackId(),
				result.role(), result.approvalStatus(), result.hubId(), result.companyId(), result.createdAt());
	}
}
