package com.delivery_project.user_service.user.infrastructure.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ의 TTL+DLX 재시도 루프를 타고 있는 행은 실패할 때마다 updatedAt이 갱신되므로,
 * 이 스케줄러가 잡는 건 애초에 발행 자체가 RabbitMQ에 도달하지 못한 드문 경우뿐이다
 * (앱 크래시, 발행 시점의 RabbitMQ 순간 장애 등). 재발행이 중복으로 일어나도 Redis 쓰기가
 * 멱등이고 컨슈머가 이미 DONE인 행은 건너뛰므로(UserInvalidationQueueConsumer 참고) 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInvalidationOutboxSweeper {

	private static final Duration STALE_THRESHOLD = Duration.ofMinutes(2);

	private final UserInvalidationOutboxRepository userInvalidationOutboxRepository;
	private final UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@Scheduled(fixedDelay = 60_000)
	public void sweep() {
		Instant threshold = Instant.now().minus(STALE_THRESHOLD);
		List<UserInvalidationOutbox> staleOutboxes = userInvalidationOutboxRepository.findPendingNotUpdatedSince(threshold);

		if (staleOutboxes.isEmpty()) {
			return;
		}

		log.warn("PENDING 상태로 방치된 무효화 아웃박스 발견. count={}", staleOutboxes.size());
		staleOutboxes.forEach(this::republish);
	}

	private void republish(UserInvalidationOutbox outbox) {
		try {
			userInvalidationQueuePublisher.publish(outbox.getId(), outbox.getTargetUserId(), outbox.getInvalidatedAt());
			log.info("방치된 무효화 아웃박스 재발행. outboxId={}", outbox.getId());
		} catch (Exception exception) {
			log.error("방치된 무효화 아웃박스 재발행 실패. outboxId={}", outbox.getId(), exception);
		}
	}
}
