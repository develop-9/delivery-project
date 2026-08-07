package com.delivery_project.user_service.user.application.command;

public record UserUpdateMeCommand(
		String name,
		String slackId
) {
}
