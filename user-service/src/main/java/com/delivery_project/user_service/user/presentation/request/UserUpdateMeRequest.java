package com.delivery_project.user_service.user.presentation.request;

import com.delivery_project.user_service.user.application.command.UserUpdateMeCommand;

public record UserUpdateMeRequest(
		String name,
		String slackId
) {
	public UserUpdateMeCommand toCommand() {
		return new UserUpdateMeCommand(name, slackId);
	}
}
