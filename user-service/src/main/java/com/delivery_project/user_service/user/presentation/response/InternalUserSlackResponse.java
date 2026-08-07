package com.delivery_project.user_service.user.presentation.response;

import java.util.UUID;

import com.delivery_project.user_service.user.application.result.InternalUserSlackResult;

public record InternalUserSlackResponse(
		UUID userId,
		String slackId
) {
	public static InternalUserSlackResponse from(InternalUserSlackResult result) {
		return new InternalUserSlackResponse(result.userId(), result.slackId());
	}
}
