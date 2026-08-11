package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis에 직접 쓰지 않고, 호출한 트랜잭션 안에서 UserInvalidationOutbox에 먼저 기록한다.
 * 평범한 DB insert라 delete/suspend/logout이 쓰는 트랜잭션에 자연스럽게 묶이므로, 호출부에서
 * 커밋 순서를 따로 신경 쓸 필요가 없다.
 *
 * RabbitMQ 발행 시도는 이 클래스가 내부적으로 커밋 이후로 미룬다 — 트랜잭션이 롤백되면
 * 애초에 존재하지 않을 아웃박스 행을 큐에 미리 알리면 안 되기 때문이다. 발행 자체가 실패해도
 * (RabbitMQ 순간 장애 등) 예외를 던지지 않는다 — 아웃박스 행은 이미 커밋되어 PENDING으로
 * 남아있으므로, 스케줄러 안전망이 나중에 다시 집어 발행한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserInvalidationRepositoryImpl implements UserInvalidationRepository {

	private final UserInvalidationOutboxRepository userInvalidationOutboxRepository;
	private final UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@Override
	public void invalidate(UUID userId, Instant invalidatedAt) {
		UserInvalidationOutbox outbox = userInvalidationOutboxRepository.save(
				UserInvalidationOutbox.create(userId, invalidatedAt));

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publishSafely(outbox.getId(), userId, invalidatedAt);
			}
		});
	}

	private void publishSafely(UUID outboxId, UUID userId, Instant invalidatedAt) {
		try {
			userInvalidationQueuePublisher.publish(outboxId, userId, invalidatedAt);
		} catch (Exception exception) {
			log.error("무효화 아웃박스 발행 실패. outboxId={} targetUserId={}", outboxId, userId, exception);
		}
	}
}
