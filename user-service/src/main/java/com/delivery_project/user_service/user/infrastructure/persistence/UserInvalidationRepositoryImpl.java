package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Redis에 직접 쓰지 않고, 호출한 트랜잭션 안에서 UserInvalidationOutbox에 먼저 기록한다.
 * 평범한 DB insert라 delete/suspend/logout이 쓰는 트랜잭션에 자연스럽게 묶이므로, 호출부에서
 * 커밋 순서를 따로 신경 쓸 필요가 없다(과거처럼 afterCommit으로 미룰 필요 없음). 실제 Redis
 * 반영은 RabbitMQ 발행/소비가 별도로 담당한다.
 */
@Repository
@RequiredArgsConstructor
public class UserInvalidationRepositoryImpl implements UserInvalidationRepository {

	private final UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@Override
	public void invalidate(UUID userId, Instant invalidatedAt) {
		userInvalidationOutboxRepository.save(UserInvalidationOutbox.create(userId, invalidatedAt));
	}
}
