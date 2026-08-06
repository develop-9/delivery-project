package com.delivery_project.user_service.user.application.command;

import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;

public record UserListCommand(
		UserSearchCondition condition,
		Pageable pageable
) {
}
