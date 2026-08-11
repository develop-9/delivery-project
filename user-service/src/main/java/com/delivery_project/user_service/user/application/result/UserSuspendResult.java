package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserSuspendResult(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserSuspendResult from(User user) {
		return new UserSuspendResult(user.getId(), user.getApprovalStatus());
	}
}
