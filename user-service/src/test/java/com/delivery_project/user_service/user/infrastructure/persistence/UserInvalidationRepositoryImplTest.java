package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

@ExtendWith(MockitoExtension.class)
class UserInvalidationRepositoryImplTest {

	@Mock
	private UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@InjectMocks
	private UserInvalidationRepositoryImpl userInvalidationRepositoryImpl;

	@Test
	void invalidate를_호출하면_Redis가_아니라_아웃박스에_PENDING_행이_저장된다() {
		// given
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();

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
}
