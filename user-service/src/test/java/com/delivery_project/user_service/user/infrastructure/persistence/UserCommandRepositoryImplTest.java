package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.delivery_project.user_service.global.config.JpaConfig;
import com.delivery_project.user_service.global.config.UserTableSchemaInitializer;
import com.delivery_project.user_service.global.crypto.AesGcmCipher;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, UserCommandRepositoryImpl.class, UserTableSchemaInitializer.class, AesGcmCipher.class})
class UserCommandRepositoryImplTest {

	@Autowired
	private UserCommandRepository userCommandRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserTableSchemaInitializer userTableSchemaInitializer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * @DataJpaTest는 ApplicationRunner를 실행시키지 않는 슬라이스 테스트라, 부분 유니크
	 * 인덱스/CHECK 제약을 보정하는 UserTableSchemaInitializer가 앱 기동 시처럼 자동
	 * 실행되지 않는다. 이 보정이 돼 있다는 걸 전제로 하는 테스트가 있어 매 테스트 전에
	 * 직접 실행해 보장한다(멱등하게 짜여 있어 반복 호출해도 안전).
	 */
	@BeforeEach
	void ensureSchemaFixups() {
		userTableSchemaInitializer.run(null);
	}

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

	@Test
	void 소프트_삭제된_사용자의_username과_slack_id는_재사용할_수_있다() {
		// given
		User original = userCommandRepository.save(createUser("reusable", "U100007"));
		original.delete(UUID.randomUUID());
		userCommandRepository.save(original);
		entityManager.flush();
		entityManager.clear();

		// when
		User reCreated = userCommandRepository.save(createUser("reusable", "U100007"));

		// then
		assertThat(reCreated.getId()).isNotEqualTo(original.getId());
		assertThat(reCreated.getUsername()).isEqualTo("reusable");
		assertThat(reCreated.getSlackId()).isEqualTo("U100007");
	}

	@Test
	void 삭제되지_않은_사용자와_같은_username이면_여전히_거부된다() {
		// given
		userCommandRepository.save(createUser("stillactive", "U100008"));
		entityManager.flush();

		// when & then
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> userCommandRepository.save(createUser("stillactive", "U100009")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 승인된_사용자를_정지_상태로_저장할_수_있다() {
		// given
		User user = userCommandRepository.save(createUser("suspendable", "U100010"));
		user.approve(UUID.randomUUID());
		userCommandRepository.save(user);

		// when
		user.suspend();
		User saved = userCommandRepository.save(user);
		entityManager.flush();

		// then
		assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
	}

	/**
	 * SUSPENDED를 ApprovalStatus에 추가하기 전부터 떠 있던 스키마를 재현한다 — approval_status
	 * CHECK 제약을 옛 값 3개(PENDING/APPROVED/REJECTED)로 되돌린 뒤, UserTableSchemaInitializer를
	 * 다시 실행해서 제약이 자동으로 SUSPENDED까지 포함하도록 보정되는지 확인한다. 이 보정이
	 * 없으면 SUSPENDED 저장 시 DataIntegrityViolationException이 발생한다(실제로 로컬 DB에서
	 * 재현됐던 문제).
	 */
	@Test
	void 옛_CHECK_제약이_남아있어도_재기동_시_SUSPENDED를_허용하도록_자동_보정된다() {
		// given: 옛 스키마 상태를 인위적으로 재현
		jdbcTemplate.execute("ALTER TABLE public.p_users DROP CONSTRAINT IF EXISTS p_users_approval_status_check");
		jdbcTemplate.execute("ALTER TABLE public.p_users ADD CONSTRAINT p_users_approval_status_check "
				+ "CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))");

		User user = userCommandRepository.save(createUser("legacyschema", "U100011"));
		user.approve(UUID.randomUUID());
		user.suspend();

		// when: 앱 재기동 시와 동일하게 스키마 보정 러너를 다시 실행
		userTableSchemaInitializer.run(null);

		// then: 보정 이전이었다면 실패했을 저장이 성공한다
		User saved = userCommandRepository.save(user);
		entityManager.flush();
		assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
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
