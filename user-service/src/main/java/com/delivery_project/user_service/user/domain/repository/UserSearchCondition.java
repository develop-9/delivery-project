package com.delivery_project.user_service.user.domain.repository;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;

/**
 * 사용자 목록 조회 조건. 모든 필드가 null 가능하며 null인 조건은 무시한다.
 */
public record UserSearchCondition(
		ApprovalStatus approvalStatus,
		Role role,
		UUID hubId,
		UUID companyId
) {
}
