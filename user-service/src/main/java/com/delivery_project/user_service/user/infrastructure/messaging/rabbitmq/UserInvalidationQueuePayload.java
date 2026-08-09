package com.delivery_project.user_service.user.infrastructure.messaging.rabbitmq;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record UserInvalidationQueuePayload(
		UUID outboxId,
		UUID targetUserId,
		Instant invalidatedAt
) implements Serializable {
}
