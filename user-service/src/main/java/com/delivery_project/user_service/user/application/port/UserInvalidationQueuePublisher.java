package com.delivery_project.user_service.user.application.port;

import java.time.Instant;
import java.util.UUID;

public interface UserInvalidationQueuePublisher {

	void publish(UUID outboxId, UUID targetUserId, Instant invalidatedAt);

	void publishRetry(UUID outboxId, UUID targetUserId, Instant invalidatedAt);
}
