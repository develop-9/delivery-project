package com.delivery_project.user_service.user.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

@ExtendWith(MockitoExtension.class)
class UserInvalidationOutboxSweeperTest {

	@Mock
	private UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@Mock
	private UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@InjectMocks
	private UserInvalidationOutboxSweeper userInvalidationOutboxSweeper;

	@Test
	void 방치된_PENDING_행이_없으면_아무것도_발행하지_않는다() {
		// given
		when(userInvalidationOutboxRepository.findPendingNotUpdatedSince(any())).thenReturn(List.of());

		// when
		userInvalidationOutboxSweeper.sweep();

		// then
		verify(userInvalidationQueuePublisher, never()).publish(any(), any(), any());
	}

	@Test
	void 방치된_PENDING_행을_찾으면_각각_재발행한다() {
		// given
		UserInvalidationOutbox outbox1 = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());
		UserInvalidationOutbox outbox2 = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());
		when(userInvalidationOutboxRepository.findPendingNotUpdatedSince(any())).thenReturn(List.of(outbox1, outbox2));

		// when
		userInvalidationOutboxSweeper.sweep();

		// then
		verify(userInvalidationQueuePublisher).publish(outbox1.getId(), outbox1.getTargetUserId(), outbox1.getInvalidatedAt());
		verify(userInvalidationQueuePublisher).publish(outbox2.getId(), outbox2.getTargetUserId(), outbox2.getInvalidatedAt());
		verify(userInvalidationQueuePublisher, times(2)).publish(any(), any(), any());
	}

	@Test
	void 한_행의_재발행이_실패해도_나머지_행은_계속_재발행한다() {
		// given
		UserInvalidationOutbox outbox1 = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());
		UserInvalidationOutbox outbox2 = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());
		when(userInvalidationOutboxRepository.findPendingNotUpdatedSince(any())).thenReturn(List.of(outbox1, outbox2));
		doThrow(new RuntimeException("RabbitMQ 연결 실패"))
				.when(userInvalidationQueuePublisher)
				.publish(outbox1.getId(), outbox1.getTargetUserId(), outbox1.getInvalidatedAt());

		// when & then
		org.assertj.core.api.Assertions.assertThatCode(() -> userInvalidationOutboxSweeper.sweep())
				.doesNotThrowAnyException();
		verify(userInvalidationQueuePublisher).publish(outbox2.getId(), outbox2.getTargetUserId(), outbox2.getInvalidatedAt());
	}
}
