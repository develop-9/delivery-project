package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	private DataSource dataSource;

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

	@Test
	void countActiveMastersForUpdate는_countActiveMasters와_같은_값을_반환한다() {
		// given
		long before = userCommandRepository.countActiveMasters();

		User master1 = userCommandRepository.save(createMaster("master-lock-1", "U100012"));
		master1.approve(UUID.randomUUID());
		userCommandRepository.save(master1);

		User master2 = userCommandRepository.save(createMaster("master-lock-2", "U100013"));
		master2.approve(UUID.randomUUID());
		userCommandRepository.save(master2);
		entityManager.flush();

		// when & then
		assertThat(userCommandRepository.countActiveMastersForUpdate()).isEqualTo(before + 2);
		assertThat(userCommandRepository.countActiveMastersForUpdate())
				.isEqualTo(userCommandRepository.countActiveMasters());
	}

	/**
	 * countActiveMastersForUpdate()가 실제로 SELECT ... FOR UPDATE로 행을 잠그는지, Hibernate
	 * 애노테이션만 믿지 않고 순수 JDBC 커넥션 두 개로 직접 검증한다. 커넥션 A가 락을 잡고
	 * 커밋하지 않은 채로, 커넥션 B가 짧은 statement_timeout으로 같은 행에 FOR UPDATE를
	 * 시도하면 대기하다 타임아웃 예외가 나야 한다 — 즉시 통과하면 락이 안 걸린 것이다.
	 *
	 * @DataJpaTest는 테스트 메서드 전체를 롤백 전용 트랜잭션으로 감싸는데, 그 안에서
	 * save()한 행은 아직 커밋되지 않아 별도 JDBC 커넥션에서는 안 보인다(READ COMMITTED).
	 * 진짜로 분리된 커넥션 두 개로 락을 검증해야 해서, 클래스 레벨 트랜잭션 감싸기를 이
	 * 메서드에서만 껐다 — 그래서 준비 단계의 save()도 즉시 커밋되고, 끝나고 나면 수동으로
	 * 지워야 한다.
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void countActiveMastersForUpdate는_실제로_행을_잠가서_동시_요청을_막는다() throws Exception {
		// given
		User master = userCommandRepository.save(createMaster("master-lock-3", "U100014"));
		master.approve(UUID.randomUUID());
		userCommandRepository.save(master);

		String lockingQuery =
				"SELECT id FROM public.p_users WHERE role = 'MASTER' AND approval_status = 'APPROVED' "
						+ "AND deleted_at IS NULL FOR UPDATE";

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch lockAcquired = new CountDownLatch(1);
		CountDownLatch releaseLock = new CountDownLatch(1);

		try (Connection connectionA = dataSource.getConnection()) {
			connectionA.setAutoCommit(false);

			Future<Boolean> holderTask = executor.submit(() -> {
				try (var statement = connectionA.createStatement()) {
					statement.executeQuery(lockingQuery);
					lockAcquired.countDown();
					releaseLock.await(5, TimeUnit.SECONDS);
				}
				return true;
			});

			assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

			// when: 두 번째 커넥션은 짧은 statement_timeout으로 같은 행에 FOR UPDATE를 시도한다.
			Future<Boolean> blockedTask = executor.submit(() -> {
				try (Connection connectionB = dataSource.getConnection()) {
					connectionB.setAutoCommit(false);
					try (var setTimeout = connectionB.createStatement()) {
						setTimeout.execute("SET LOCAL statement_timeout = '1000ms'");
					}
					try (var statement = connectionB.createStatement()) {
						statement.executeQuery(lockingQuery);
						return false; // 락에 안 걸리고 바로 통과했다면 실패
					} catch (SQLException e) {
						return true; // 타임아웃으로 막힌 것이 기대하는 결과
					} finally {
						connectionB.rollback();
					}
				}
			});

			// then
			assertThat(blockedTask.get(5, TimeUnit.SECONDS)).isTrue();

			releaseLock.countDown();
			holderTask.get(5, TimeUnit.SECONDS);
			connectionA.rollback();
		} finally {
			executor.shutdownNow();
			// 이 메서드는 트랜잭션 감싸기를 껐으니 save()가 이미 커밋됐다 — 직접 지운다.
			jdbcTemplate.update("DELETE FROM public.p_users WHERE username = 'master-lock-3'");
		}
	}

	private User createUser(String username, String slackId) {
		return User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId(slackId)
				.role(Role.COMPANY_MANAGER)
				.companyId(UUID.randomUUID())
				.build();
	}

	private User createMaster(String username, String slackId) {
		return User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId(slackId)
				.role(Role.MASTER)
				.build();
	}
}
