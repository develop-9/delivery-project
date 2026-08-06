package com.delivery_project.user_service.user.application.command;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

public record UserSignupCommand(
		String username,
		String password,
		String name,
		String slackId,
		Role role,
		UUID hubId,
		UUID companyId
) {
}
