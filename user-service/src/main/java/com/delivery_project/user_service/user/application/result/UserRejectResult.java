package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserRejectResult(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserRejectResult from(User user) {
		return new UserRejectResult(user.getId(), user.getApprovalStatus());
	}
}
