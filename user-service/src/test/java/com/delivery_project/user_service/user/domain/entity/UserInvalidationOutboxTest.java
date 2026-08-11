package com.delivery_project.user_service.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserInvalidationOutboxTest {

	@Test
	void 생성하면_PENDING_상태이고_시도횟수는_0이다() {
		// given
		UUID targetUserId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();

		// when
		UserInvalidationOutbox outbox = UserInvalidationOutbox.create(targetUserId, invalidatedAt);

		// then
		assertThat(outbox.getTargetUserId()).isEqualTo(targetUserId);
		assertThat(outbox.getInvalidatedAt()).isEqualTo(invalidatedAt);
		assertThat(outbox.getStatus()).isEqualTo(UserInvalidationOutboxStatus.PENDING);
		assertThat(outbox.getAttemptCount()).isZero();
		assertThat(outbox.isPending()).isTrue();
	}

	@Test
	void markDone을_호출하면_DONE_상태가_된다() {
		// given
		UserInvalidationOutbox outbox = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());

		// when
		outbox.markDone();

		// then
		assertThat(outbox.getStatus()).isEqualTo(UserInvalidationOutboxStatus.DONE);
		assertThat(outbox.isPending()).isFalse();
	}

	@Test
	void recordAttemptFailure를_호출하면_시도횟수만_증가하고_PENDING_상태는_유지된다() {
		// given
		UserInvalidationOutbox outbox = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());

		// when
		outbox.recordAttemptFailure();
		outbox.recordAttemptFailure();

		// then
		assertThat(outbox.getAttemptCount()).isEqualTo(2);
		assertThat(outbox.isPending()).isTrue();
	}
}
