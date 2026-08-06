package com.delivery_project.user_service.user.application.query;

import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;

public record UserListQuery(
		UserSearchCondition condition,
		Pageable pageable
) {
}
