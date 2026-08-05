package com.delivery_project.user_service.user.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public record UserApproveResult(
		UUID userId,
		ApprovalStatus approvalStatus,
		Instant approvedAt,
		UUID approvedBy
) {
	public static UserApproveResult from(User user) {
		return new UserApproveResult(user.getId(), user.getApprovalStatus(), user.getApprovedAt(), user.getApprovedBy());
	}
}
