package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;

public record UserRejectResponse(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserRejectResponse from(UserRejectResult result) {
		return new UserRejectResponse(result.userId(), result.approvalStatus());
	}
}
