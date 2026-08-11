package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserReinstateResult(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserReinstateResult from(User user) {
		return new UserReinstateResult(user.getId(), user.getApprovalStatus());
	}
}
