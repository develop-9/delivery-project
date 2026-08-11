package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * compareAndRotate()의 원자성(Compare-And-Swap)은 Mockito로 검증할 수 없다 — Lua 스크립트가
 * 실제로 Redis 안에서 원자적으로 실행되는지를 확인하려면 진짜 Redis가 필요하다. 테스트
 * application.yaml과 같은 Redis(localhost:6379)에 직접 붙어서 검증한다.
 */
class RefreshTokenRepositoryImplTest {

	private LettuceConnectionFactory connectionFactory;
	private StringRedisTemplate redisTemplate;
	private RefreshTokenRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		connectionFactory = new LettuceConnectionFactory("localhost", 6379);
		connectionFactory.afterPropertiesSet();
		redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		repository = new RefreshTokenRepositoryImpl(redisTemplate);
	}

	@AfterEach
	void tearDown() {
		connectionFactory.destroy();
	}

	@Test
	void save한_토큰은_같은_세션에서_존재로_확인된다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		// when
		repository.save(userId, sessionId, "token-1", Duration.ofMinutes(5));

		// then
		try {
			assertThat(repository.existsByUserIdAndSessionId(userId, sessionId)).isTrue();
		} finally {
			repository.deleteAllByUserId(userId);
		}
	}

	@Test
	void compareAndRotate는_저장된_값과_같을_때만_성공하고_새_값으로_교체한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		repository.save(userId, sessionId, "old-token", Duration.ofMinutes(5));

		try {
			// when
			boolean rotated =
					repository.compareAndRotate(userId, sessionId, "old-token", "new-token", Duration.ofMinutes(5));

			// then
			assertThat(rotated).isTrue();
			// 새 값으로 실제로 바뀌었는지는 그 값으로 다시 CAS가 성공하는지로 간접 확인한다
			assertThat(repository.compareAndRotate(userId, sessionId, "new-token", "newer-token", Duration.ofMinutes(5)))
					.isTrue();
		} finally {
			repository.deleteAllByUserId(userId);
		}
	}

	@Test
	void compareAndRotate는_저장된_값과_다르면_실패하고_값을_바꾸지_않는다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		repository.save(userId, sessionId, "old-token", Duration.ofMinutes(5));

		try {
			// when
			boolean rotated =
					repository.compareAndRotate(userId, sessionId, "wrong-token", "new-token", Duration.ofMinutes(5));

			// then
			assertThat(rotated).isFalse();
			// 값이 안 바뀌었으므로 원래 값으로는 여전히 CAS가 성공해야 한다
			assertThat(repository.compareAndRotate(userId, sessionId, "old-token", "new-token", Duration.ofMinutes(5)))
					.isTrue();
		} finally {
			repository.deleteAllByUserId(userId);
		}
	}

	@Test
	void 세션이_없으면_compareAndRotate는_실패한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		// when & then
		assertThat(repository.compareAndRotate(userId, sessionId, "any-token", "new-token", Duration.ofMinutes(5)))
				.isFalse();
	}

	/**
	 * 실제 레이스 컨디션 재현 — 같은 저장된 값을 두고 20개 스레드가 동시에 compareAndRotate를
	 * 시도하면, Lua 스크립트가 Redis 안에서 원자적으로 실행되므로 정확히 하나만 성공해야 한다.
	 * 동시 재발급 요청 레이스 컨디션을 막는 게 이번 작업의 핵심이라 이 테스트가 가장 중요하다.
	 */
	@Test
	void 동시에_같은_토큰으로_재발급_요청이_여러번_와도_정확히_하나만_성공한다() throws InterruptedException {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		repository.save(userId, sessionId, "old-token", Duration.ofMinutes(5));

		int threadCount = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();

		try {
			// when: 20개 스레드가 전부 같은 old-token을 들고 동시에 재발급을 시도한다
			for (int i = 0; i < threadCount; i++) {
				int idx = i;
				executor.submit(() -> {
					ready.countDown();
					try {
						start.await();
						boolean rotated = repository.compareAndRotate(
								userId, sessionId, "old-token", "new-token-" + idx, Duration.ofMinutes(5));
						if (rotated) {
							successCount.incrementAndGet();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						done.countDown();
					}
				});
			}
			ready.await();
			start.countDown();
			done.await(10, TimeUnit.SECONDS);

			// then
			assertThat(successCount.get()).isEqualTo(1);
		} finally {
			executor.shutdown();
			repository.deleteAllByUserId(userId);
		}
	}

	@Test
	void deleteAllByUserId는_그_사용자의_모든_세션을_제거한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId1 = UUID.randomUUID();
		UUID sessionId2 = UUID.randomUUID();
		repository.save(userId, sessionId1, "token-1", Duration.ofMinutes(5));
		repository.save(userId, sessionId2, "token-2", Duration.ofMinutes(5));

		// when
		repository.deleteAllByUserId(userId);

		// then
		assertThat(repository.existsByUserIdAndSessionId(userId, sessionId1)).isFalse();
		assertThat(repository.existsByUserIdAndSessionId(userId, sessionId2)).isFalse();
	}

	@Test
	void deleteByUserIdAndSessionIdOrThrow는_그_세션만_제거하고_다른_기기_세션은_그대로_둔다() {
		// given: 같은 사용자가 두 기기에서 로그인한 상황을 재현한다
		UUID userId = UUID.randomUUID();
		UUID sessionId1 = UUID.randomUUID();
		UUID sessionId2 = UUID.randomUUID();
		repository.save(userId, sessionId1, "token-1", Duration.ofMinutes(5));
		repository.save(userId, sessionId2, "token-2", Duration.ofMinutes(5));

		try {
			// when: 기기 1에서만 로그아웃한다
			repository.deleteByUserIdAndSessionIdOrThrow(userId, sessionId1);

			// then: 기기 1의 세션만 사라지고, 기기 2의 세션은 영향받지 않는다
			assertThat(repository.existsByUserIdAndSessionId(userId, sessionId1)).isFalse();
			assertThat(repository.existsByUserIdAndSessionId(userId, sessionId2)).isTrue();
		} finally {
			repository.deleteAllByUserId(userId);
		}
	}

	@Test
	void blacklistSessionOrThrow는_별도_키에_등록될_뿐_RefreshToken_자체는_건드리지_않는다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		repository.save(userId, sessionId, "token-1", Duration.ofMinutes(5));

		try {
			// when
			repository.blacklistSessionOrThrow(userId, sessionId, Duration.ofMinutes(5));

			// then: API Gateway가 확인하는 세션 블랙리스트 키가 실제로 생겼는지 확인한다
			// (Gateway 쪽 키 이름과 반드시 일치해야 하는 계약이라 여기서 고정값으로 검증한다)
			assertThat(redisTemplate.hasKey("session:" + userId + ":" + sessionId + ":blacklisted")).isTrue();
			// RefreshToken 자체는 별도 키라 영향받지 않는다
			assertThat(repository.existsByUserIdAndSessionId(userId, sessionId)).isTrue();
		} finally {
			repository.deleteAllByUserId(userId);
			redisTemplate.delete("session:" + userId + ":" + sessionId + ":blacklisted");
		}
	}
}
