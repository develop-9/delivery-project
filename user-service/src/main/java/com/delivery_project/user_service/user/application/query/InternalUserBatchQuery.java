package com.delivery_project.user_service.user.application.query;

import java.util.Collection;
import java.util.UUID;

public record InternalUserBatchQuery(
		Collection<UUID> userIds
) {
}
