package com.delivery_project.user_service.user.application.command;

import java.util.Collection;
import java.util.UUID;

public record InternalUserBatchCommand(
		Collection<UUID> userIds
) {
}
