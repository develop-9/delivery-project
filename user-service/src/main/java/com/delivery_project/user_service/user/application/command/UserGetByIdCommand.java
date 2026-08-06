package com.delivery_project.user_service.user.application.command;

import java.util.UUID;

public record UserGetByIdCommand(
		UUID targetUserId
) {
}
