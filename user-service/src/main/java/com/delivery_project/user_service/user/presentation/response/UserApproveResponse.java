package com.delivery_project.user_service.user.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;

public record UserApproveResponse(
		UUID userId,
		ApprovalStatus approvalStatus,
		Instant approvedAt,
		UUID approvedBy
) {
	public static UserApproveResponse from(UserApproveResult result) {
		return new UserApproveResponse(result.userId(), result.approvalStatus(), result.approvedAt(), result.approvedBy());
	}
}
