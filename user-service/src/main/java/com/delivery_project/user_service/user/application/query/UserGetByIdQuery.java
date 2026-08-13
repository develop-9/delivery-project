package com.delivery_project.user_service.user.application.query;

import java.util.UUID;

public record UserGetByIdQuery(
		UUID targetUserId
) {
}
