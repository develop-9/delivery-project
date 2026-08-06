package com.delivery_project.user_service.user.application.command;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

public record UserPendingCommand(
		UUID hubId,
		Pageable pageable
) {
}
