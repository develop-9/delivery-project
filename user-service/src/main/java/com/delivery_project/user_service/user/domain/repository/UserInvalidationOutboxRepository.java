package com.delivery_project.user_service.user.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;

/**
 * Access Token 무효화 아웃박스 저장소 포트. RabbitMQ 발행이 원자적으로 DB 트랜잭션에 묶이지
 * 않으므로, 실제 발행 성공 여부와 무관하게 삭제/정지/로그아웃 트랜잭션 안에서 먼저 이 저장소에
 * 기록한다.
 */
public interface UserInvalidationOutboxRepository {

	UserInvalidationOutbox save(UserInvalidationOutbox outbox);

	Optional<UserInvalidationOutbox> findById(UUID id);

	/**
	 * updatedAt이 threshold보다 오래된 PENDING 행을 찾는다. RabbitMQ의 TTL+DLX 재시도 루프를
	 * 타고 있는 행은 실패할 때마다 updatedAt이 갱신되므로 이 조건에 걸리지 않는다 — 이 조건에
	 * 걸리는 행은 애초에 발행 자체가 RabbitMQ에 도달하지 못한(앱 크래시 등) 드문 경우다.
	 */
	List<UserInvalidationOutbox> findPendingNotUpdatedSince(Instant threshold);
}
