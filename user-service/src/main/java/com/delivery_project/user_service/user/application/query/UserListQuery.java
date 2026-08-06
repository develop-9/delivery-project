package com.delivery_project.user_service.user.application.query;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;

public record UserListQuery(
		ApprovalStatus approvalStatus,
		Role role,
		UUID hubId,
		UUID companyId,
		Pageable pageable
) {
}
