package com.delivery_project.user_service.user.domain.entity;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.user_service.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 삭제/정지/로그아웃으로 발생한 Access Token 무효화를 Redis에 즉시 쓰지 않고 같은 DB
 * 트랜잭션 안에 먼저 기록해두는 아웃박스. Redis가 그 순간 죽어 있어도 삭제/정지/로그아웃
 * 자체는 항상 성공하고, 실제 Redis 반영은 RabbitMQ 컨슈머가 별도로 담당한다.
 *
 * SlackMessage와 달리 MAX_RETRY_COUNT로 포기하는 FAILED 상태가 없다 — Access Token을
 * 막는 유일한 방어선이라 반드시 결국엔 Redis에 반영돼야 하므로, RabbitMQ의 TTL+DLX
 * 재시도 루프가 성공할 때까지 계속 돌게 두고 attemptCount/updatedAt은 관찰용으로만 쓴다.
 */
@Getter
@Entity
@Table(name = "p_user_invalidation_outboxes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInvalidationOutbox extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "target_user_id", nullable = false)
	private UUID targetUserId;

	@Column(name = "invalidated_at", nullable = false)
	private Instant invalidatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserInvalidationOutboxStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	private UserInvalidationOutbox(UUID targetUserId, Instant invalidatedAt) {
		this.targetUserId = targetUserId;
		this.invalidatedAt = invalidatedAt;
		this.status = UserInvalidationOutboxStatus.PENDING;
		this.attemptCount = 0;
		this.updatedAt = Instant.now();
	}

	public static UserInvalidationOutbox create(UUID targetUserId, Instant invalidatedAt) {
		return new UserInvalidationOutbox(targetUserId, invalidatedAt);
	}

	public void markDone() {
		this.status = UserInvalidationOutboxStatus.DONE;
		this.updatedAt = Instant.now();
	}

	public void recordAttemptFailure() {
		this.attemptCount++;
		this.updatedAt = Instant.now();
	}

	public boolean isPending() {
		return status == UserInvalidationOutboxStatus.PENDING;
	}
}
