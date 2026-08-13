package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserReinstateResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;

public record UserReinstateResponse(
		UUID userId,
		ApprovalStatus approvalStatus
) {
	public static UserReinstateResponse from(UserReinstateResult result) {
		return new UserReinstateResponse(result.userId(), result.approvalStatus());
	}
}
