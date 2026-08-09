package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.User;

public record InternalUserSlackResult(
		UUID userId,
		String slackId
) {
	public static InternalUserSlackResult from(User user) {
		return new InternalUserSlackResult(user.getId(), user.getSlackId());
	}
}
