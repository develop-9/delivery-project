package com.delivery_project.user_service.user.application.command;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

public record InternalUserHubRoleCommand(
		UUID hubId,
		Role role
) {
}
