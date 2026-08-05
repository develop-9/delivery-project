package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

/** GET /users/me, GET /users/{userId}가 공유하는 상세 조회 결과. */
public record UserDetailResult(
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
	public static UserDetailResult from(User user) {
		return new UserDetailResult(
				user.getId(), user.getUsername(), user.getName(), user.getSlackId(),
				user.getRole(), user.getApprovalStatus(), user.getHubId(), user.getCompanyId(), user.getCreatedAt());
	}
}
