package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserSignupResult(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserSignupResult from(User user) {
		return new UserSignupResult(user.getId(), user.getApprovalStatus());
	}
}
