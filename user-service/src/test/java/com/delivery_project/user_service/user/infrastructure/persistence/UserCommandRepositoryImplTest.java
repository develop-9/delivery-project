package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.delivery_project.user_service.global.config.JpaConfig;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, UserCommandRepositoryImpl.class})
class UserCommandRepositoryImplTest {

	@Autowired
	private UserCommandRepository userCommandRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자를_저장하면_승인상태는_기본값으로_PENDING이_된다() {
		// given
		User user = createUser("pendinguser", "U100001");

		// when
		User saved = userCommandRepository.save(user);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getApprovalStatus().name()).isEqualTo("PENDING");
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void username으로_사용자를_조회할_수_있다() {
		// given
		userCommandRepository.save(createUser("finduser", "U100002"));

		// when
		Optional<User> found = userCommandRepository.findByUsername("finduser");

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("finduser");
	}

	@Test
	void 존재하지_않는_username으로_조회하면_비어있다() {
		// when
		Optional<User> found = userCommandRepository.findByUsername("no-such-user");

		// then
		assertThat(found).isEmpty();
	}

	@Test
	void existsByUsername으로_중복여부를_확인할_수_있다() {
		// given
		userCommandRepository.save(createUser("dupcheck", "U100003"));

		// when & then
		assertThat(userCommandRepository.existsByUsername("dupcheck")).isTrue();
		assertThat(userCommandRepository.existsByUsername("nobody")).isFalse();
	}

	@Test
	void existsBySlackId로_중복여부를_확인할_수_있다() {
		// given
		userCommandRepository.save(createUser("slackcheck", "U100004"));

		// when & then
		assertThat(userCommandRepository.existsBySlackId("U100004")).isTrue();
		assertThat(userCommandRepository.existsBySlackId("U999999")).isFalse();
	}

	@Test
	void 소프트_삭제된_사용자는_findById로_조회되지_않는다() {
		// given
		User user = userCommandRepository.save(createUser("deleteduser", "U100005"));
		UUID userId = user.getId();

		user.delete(UUID.randomUUID());
		userCommandRepository.save(user);
		entityManager.flush();
		entityManager.clear();

		// when
		Optional<User> found = userCommandRepository.findById(userId);

		// then
		assertThat(found).isEmpty();
	}

	@Test
	void 소프트_삭제된_사용자는_findByUsername으로도_조회되지_않는다() {
		// given
		userCommandRepository.save(createUser("deletedbyname", "U100006"));
		User user = userCommandRepository.findByUsername("deletedbyname").orElseThrow();

		user.delete(UUID.randomUUID());
		userCommandRepository.save(user);
		entityManager.flush();
		entityManager.clear();

		// when
		Optional<User> found = userCommandRepository.findByUsername("deletedbyname");

		// then
		assertThat(found).isEmpty();
	}

	private User createUser(String username, String slackId) {
		return User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId(slackId)
				.role(Role.COMPANY_MANAGER)
				.build();
	}
}
