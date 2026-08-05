package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;

public record UserSignupResponse(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserSignupResponse from(UserSignupResult result) {
		return new UserSignupResponse(result.userId(), result.approvalStatus());
	}
}
