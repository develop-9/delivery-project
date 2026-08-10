package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserSuspendResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;

public record UserSuspendResponse(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserSuspendResponse from(UserSuspendResult result) {
		return new UserSuspendResponse(result.userId(), result.approvalStatus());
	}
}
