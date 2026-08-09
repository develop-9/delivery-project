package com.delivery_project.user_service.user.application.query;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

public record InternalUserHubRoleQuery(
		UUID hubId,
		Role role
) {
}
