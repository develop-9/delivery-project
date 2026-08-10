package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.delivery_project.user_service.user.application.port.UserInvalidationQueuePublisher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

@ExtendWith(MockitoExtension.class)
class UserInvalidationRepositoryImplTest {

	@Mock
	private UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@Mock
	private UserInvalidationQueuePublisher userInvalidationQueuePublisher;

	@InjectMocks
	private UserInvalidationRepositoryImpl userInvalidationRepositoryImpl;

	@BeforeEach
	void setUpTransactionSynchronization() {
		TransactionSynchronizationManager.initSynchronization();
	}

	@AfterEach
	void clearTransactionSynchronization() {
		TransactionSynchronizationManager.clearSynchronization();
	}

	private void simulateCommit() {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
	}

	@Test
	void invalidate를_호출하면_Redis가_아니라_아웃박스에_PENDING_행이_저장된다() {
		// given
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();
		when(userInvalidationOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		userInvalidationRepositoryImpl.invalidate(userId, invalidatedAt);

		// then
		ArgumentCaptor<UserInvalidationOutbox> captor = ArgumentCaptor.forClass(UserInvalidationOutbox.class);
		verify(userInvalidationOutboxRepository).save(captor.capture());

		UserInvalidationOutbox saved = captor.getValue();
		assertThat(saved.getTargetUserId()).isEqualTo(userId);
		assertThat(saved.getInvalidatedAt()).isEqualTo(invalidatedAt);
		assertThat(saved.isPending()).isTrue();
	}

	@Test
	void 트랜잭션_커밋_전에는_RabbitMQ에_발행하지_않는다() {
		// given
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();
		when(userInvalidationOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		userInvalidationRepositoryImpl.invalidate(userId, invalidatedAt);

		// then: 아직 커밋 전이라 트랜잭션이 롤백될 수도 있으므로 발행하지 않는다.
		verify(userInvalidationQueuePublisher, never()).publish(any(), any(), any());
	}

	@Test
	void 트랜잭션이_커밋되면_아웃박스_id와_함께_RabbitMQ에_발행한다() {
		// given
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();
		when(userInvalidationOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		userInvalidationRepositoryImpl.invalidate(userId, invalidatedAt);
		simulateCommit();

		// then
		verify(userInvalidationQueuePublisher).publish(any(), eq(userId), eq(invalidatedAt));
	}

	@Test
	void 커밋_이후_발행이_실패해도_예외가_전파되지_않는다() {
		// given
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();
		when(userInvalidationOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		org.mockito.Mockito.doThrow(new RuntimeException("RabbitMQ 연결 실패"))
				.when(userInvalidationQueuePublisher).publish(any(), any(), any());

		// when & then: 발행 실패는 아웃박스 행이 이미 PENDING으로 커밋돼 있어 안전망이 처리하므로,
		// 여기서 예외가 올라와 afterCommit 콜백 체인 전체를 깨뜨리면 안 된다.
		userInvalidationRepositoryImpl.invalidate(userId, invalidatedAt);
		org.assertj.core.api.Assertions.assertThatCode(this::simulateCommit).doesNotThrowAnyException();
	}
}
