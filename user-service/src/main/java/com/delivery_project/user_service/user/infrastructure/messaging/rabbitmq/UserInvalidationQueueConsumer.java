package com.delivery_project.user_service.user.infrastructure.messaging.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;
import com.delivery_project.user_service.user.infrastructure.config.UserInvalidationRabbitMqConfig;
import com.delivery_project.user_service.user.infrastructure.persistence.RedisUserInvalidationWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아웃박스에 쌓인 Access Token 무효화 요청을 실제로 Redis에 반영한다. Slack Service의
 * SlackMessageQueueConsumer와 달리 중복 소비 가드가 없다 — Redis에 같은 값을 두 번 쓰는 건
 * 멱등이라(SET은 있으면 덮어쓰기), 재전송으로 인한 중복 처리가 실제 사용자에게 영향을 주는
 * Slack 발송과 성격이 다르다.
 *
 * 재시도에 상한이 없다 — 실패하면 언제나 RETRY_QUEUE로 다시 보내고, PENDING 상태를 유지한다.
 * Access Token을 막는 유일한 방어선이라 결국엔 반영돼야 하므로, SlackMessage처럼 포기하는
 * FAILED 상태를 두지 않는다(UserInvalidationOutbox 참고).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInvalidationQueueConsumer {

	private final RedisUserInvalidationWriter redisUserInvalidationWriter;
	private final UserInvalidationOutboxRepository userInvalidationOutboxRepository;
	private final UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@RabbitListener(queues = UserInvalidationRabbitMqConfig.MAIN_QUEUE)
	public void consume(UserInvalidationQueuePayload payload) {
		UserInvalidationOutbox outbox = userInvalidationOutboxRepository.findById(payload.outboxId())
				.orElse(null);

		if (outbox == null) {
			log.warn("존재하지 않는 아웃박스 행. outboxId={}", payload.outboxId());
			return;
		}

		if (!outbox.isPending()) {
			log.info("이미 처리된 무효화 요청. outboxId={} status={}", outbox.getId(), outbox.getStatus());
			return;
		}

		try {
			redisUserInvalidationWriter.write(payload.targetUserId(), payload.invalidatedAt());
			outbox.markDone();
			userInvalidationOutboxRepository.save(outbox);

			log.info("Access Token 무효화 반영 완료. outboxId={} targetUserId={}", outbox.getId(), payload.targetUserId());
		} catch (Exception exception) {
			outbox.recordAttemptFailure();
			userInvalidationOutboxRepository.save(outbox);

			log.warn("Access Token 무효화 반영 실패. outboxId={} targetUserId={} attemptCount={}",
					outbox.getId(), payload.targetUserId(), outbox.getAttemptCount(), exception);

			publishRetrySafely(payload);
		}
	}

	/**
	 * Retry Queue 발행 자체가 실패하면(RabbitMQ 순간 장애 등) 이 메시지는 큐에서 사라지지만,
	 * 아웃박스 행은 여전히 PENDING이라 스케줄러 안전망이 나중에 다시 집어 발행한다.
	 */
	private void publishRetrySafely(UserInvalidationQueuePayload payload) {
		try {
			userInvalidationQueuePublisher.publishRetry(
					payload.outboxId(), payload.targetUserId(), payload.invalidatedAt());
		} catch (Exception exception) {
			log.error("Retry Queue 발행 실패. outboxId={}", payload.outboxId(), exception);
		}
	}
}
