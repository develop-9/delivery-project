package com.delivery_project.user_service.user.application.query;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

public record UserPendingQuery(
		UUID hubId,
		Pageable pageable
) {
}
