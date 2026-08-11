package com.delivery_project.user_service.user.infrastructure.messaging.rabbitmq;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.infrastructure.config.UserInvalidationRabbitMqConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserInvalidationQueuePublisherImpl implements UserInvalidationQueuePublisher {

	private final RabbitTemplate rabbitTemplate;

	@Override
	public void publish(UUID outboxId, UUID targetUserId, Instant invalidatedAt) {
		rabbitTemplate.convertAndSend(
				UserInvalidationRabbitMqConfig.EXCHANGE,
				UserInvalidationRabbitMqConfig.MAIN_ROUTING_KEY,
				new UserInvalidationQueuePayload(outboxId, targetUserId, invalidatedAt));
	}

	@Override
	public void publishRetry(UUID outboxId, UUID targetUserId, Instant invalidatedAt) {
		rabbitTemplate.convertAndSend(
				UserInvalidationRabbitMqConfig.EXCHANGE,
				UserInvalidationRabbitMqConfig.RETRY_ROUTING_KEY,
				new UserInvalidationQueuePayload(outboxId, targetUserId, invalidatedAt));
	}
}
