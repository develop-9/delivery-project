package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.delivery_project.user_service.global.config.JpaConfig;
import com.delivery_project.user_service.global.crypto.AesGcmCipher;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, UserInvalidationOutboxRepositoryImpl.class, AesGcmCipher.class})
class UserInvalidationOutboxRepositoryImplTest {

	@Autowired
	private UserInvalidationOutboxRepository userInvalidationOutboxRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 저장하면_id와_생성시각이_채워진다() {
		// given
		UserInvalidationOutbox outbox = UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now());

		// when
		UserInvalidationOutbox saved = userInvalidationOutboxRepository.save(outbox);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void id로_조회할_수_있다() {
		// given
		UserInvalidationOutbox saved = userInvalidationOutboxRepository.save(
				UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now()));

		// when
		Optional<UserInvalidationOutbox> found = userInvalidationOutboxRepository.findById(saved.getId());

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(saved.getId());
	}

	@Test
	void updatedAt이_threshold보다_오래된_PENDING_행만_조회된다() {
		// given: 오래 방치된 PENDING, 최근 갱신된 PENDING, DONE 세 가지 상황을 만든다.
		UserInvalidationOutbox stale = userInvalidationOutboxRepository.save(
				UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now()));
		UserInvalidationOutbox recentlyUpdated = userInvalidationOutboxRepository.save(
				UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now()));
		UserInvalidationOutbox done = userInvalidationOutboxRepository.save(
				UserInvalidationOutbox.create(UUID.randomUUID(), Instant.now()));
		done.markDone();
		userInvalidationOutboxRepository.save(done);
		entityManager.flush();

		// stale의 updated_at을 과거로 직접 되돌려, "재시도 루프를 타지 못하고 방치된" 상태를 재현한다.
		entityManager.getEntityManager()
				.createQuery("update UserInvalidationOutbox o set o.updatedAt = :past where o.id = :id")
				.setParameter("past", Instant.now().minus(10, ChronoUnit.MINUTES))
				.setParameter("id", stale.getId())
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();

		// when
		List<UserInvalidationOutbox> result = userInvalidationOutboxRepository.findPendingNotUpdatedSince(
				Instant.now().minus(1, ChronoUnit.MINUTES));

		// then
		assertThat(result).extracting(UserInvalidationOutbox::getId)
				.containsExactly(stale.getId())
				.doesNotContain(recentlyUpdated.getId(), done.getId());
	}
}
