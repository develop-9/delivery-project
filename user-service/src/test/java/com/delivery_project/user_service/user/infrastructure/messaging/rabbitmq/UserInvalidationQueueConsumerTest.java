package com.delivery_project.user_service.user.infrastructure.messaging.rabbitmq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;
import com.delivery_project.user_service.user.infrastructure.persistence.RedisUserInvalidationWriter;

@ExtendWith(MockitoExtension.class)
class UserInvalidationQueueConsumerTest {

	@Mock
	private RedisUserInvalidationWriter redisUserInvalidationWriter;

	@Mock
	private UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@Mock
	private UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@InjectMocks
	private UserInvalidationQueueConsumer userInvalidationQueueConsumer;

	private final UUID outboxId = UUID.randomUUID();
	private final UUID targetUserId = UUID.randomUUID();
	private final Instant invalidatedAt = Instant.now();

	private UserInvalidationQueuePayload payload() {
		return new UserInvalidationQueuePayload(outboxId, targetUserId, invalidatedAt);
	}

	private UserInvalidationOutbox pendingOutbox() {
		UserInvalidationOutbox outbox = UserInvalidationOutbox.create(targetUserId, invalidatedAt);
		org.springframework.test.util.ReflectionTestUtils.setField(outbox, "id", outboxId);
		return outbox;
	}

	@Test
	void Redis_반영에_성공하면_아웃박스를_DONE으로_저장하고_재시도하지_않는다() {
		// given
		UserInvalidationOutbox outbox = pendingOutbox();
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));

		// when
		userInvalidationQueueConsumer.consume(payload());

		// then
		verify(redisUserInvalidationWriter).write(targetUserId, invalidatedAt);
		verify(userInvalidationOutboxRepository).save(outbox);
		org.assertj.core.api.Assertions.assertThat(outbox.isPending()).isFalse();
		verify(userInvalidationQueuePublisher, never()).publishRetry(any(), any(), any());
	}

	@Test
	void Redis_반영에_실패하면_시도횟수를_기록하고_Retry_Queue에_발행한다() {
		// given
		UserInvalidationOutbox outbox = pendingOutbox();
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
		doThrow(new RuntimeException("Redis 연결 실패")).when(redisUserInvalidationWriter).write(targetUserId, invalidatedAt);

		// when
		userInvalidationQueueConsumer.consume(payload());

		// then
		verify(userInvalidationOutboxRepository).save(outbox);
		org.assertj.core.api.Assertions.assertThat(outbox.isPending()).isTrue();
		org.assertj.core.api.Assertions.assertThat(outbox.getAttemptCount()).isEqualTo(1);
		verify(userInvalidationQueuePublisher).publishRetry(outboxId, targetUserId, invalidatedAt);
	}

	@Test
	void Retry_Queue_발행이_실패해도_예외가_전파되지_않는다() {
		// given
		UserInvalidationOutbox outbox = pendingOutbox();
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
		doThrow(new RuntimeException("Redis 연결 실패")).when(redisUserInvalidationWriter).write(targetUserId, invalidatedAt);
		doThrow(new RuntimeException("RabbitMQ 연결 실패"))
				.when(userInvalidationQueuePublisher).publishRetry(any(), any(), any());

		// when & then: 발행 실패는 아웃박스 행이 PENDING으로 남아 스케줄러 안전망이 처리하므로,
		// 여기서 예외가 올라와 리스너 컨테이너의 재전송/재시도 흐름을 흔들면 안 된다.
		org.assertj.core.api.Assertions.assertThatCode(() -> userInvalidationQueueConsumer.consume(payload()))
				.doesNotThrowAnyException();
	}

	@Test
	void 존재하지_않는_아웃박스_행이면_아무것도_하지_않는다() {
		// given
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.empty());

		// when
		userInvalidationQueueConsumer.consume(payload());

		// then
		verify(redisUserInvalidationWriter, never()).write(any(), any());
		verify(userInvalidationOutboxRepository, never()).save(any());
	}

	@Test
	void 이미_DONE인_아웃박스_행이면_다시_처리하지_않는다() {
		// given
		UserInvalidationOutbox outbox = pendingOutbox();
		outbox.markDone();
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));

		// when
		userInvalidationQueueConsumer.consume(payload());

		// then
		verify(redisUserInvalidationWriter, never()).write(any(), any());
		verify(userInvalidationOutboxRepository, never()).save(any());
	}

	/**
	 * findById() 자체가 DB 장애로 실패해도 예외가 밖으로 안 나가야 한다 — 나가면
	 * spring-rabbit 기본 동작(즉시 requeue, 백오프 없음)에 넘어가서 설계된 5초 TTL
	 * 재시도를 건너뛰고 DB 장애 중에 오히려 더 몰아치게 된다.
	 */
	@Test
	void findById_자체가_DB_장애로_실패해도_예외가_전파되지_않고_재발행은_그대로_나간다() {
		// given
		doThrow(new RuntimeException("DB 연결 실패")).when(userInvalidationOutboxRepository).findById(outboxId);

		// when & then
		org.assertj.core.api.Assertions.assertThatCode(() -> userInvalidationQueueConsumer.consume(payload()))
				.doesNotThrowAnyException();

		verify(redisUserInvalidationWriter, never()).write(any(), any());
		verify(userInvalidationQueuePublisher).publishRetry(outboxId, targetUserId, invalidatedAt);
	}

	/**
	 * write() 실패 이후 시도횟수 기록용 save()마저 실패해도(연쇄 DB 장애) 예외가 전파되면
	 * 안 되고, 재발행은 그래도 나가야 한다.
	 */
	@Test
	void 시도횟수_기록_저장이_실패해도_예외가_전파되지_않고_재발행은_그대로_나간다() {
		// given
		UserInvalidationOutbox outbox = pendingOutbox();
		when(userInvalidationOutboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
		doThrow(new RuntimeException("Redis 연결 실패")).when(redisUserInvalidationWriter).write(targetUserId, invalidatedAt);
		doThrow(new RuntimeException("DB 연결 실패")).when(userInvalidationOutboxRepository).save(outbox);

		// when & then
		org.assertj.core.api.Assertions.assertThatCode(() -> userInvalidationQueueConsumer.consume(payload()))
				.doesNotThrowAnyException();

		verify(userInvalidationQueuePublisher).publishRetry(outboxId, targetUserId, invalidatedAt);
	}
}
