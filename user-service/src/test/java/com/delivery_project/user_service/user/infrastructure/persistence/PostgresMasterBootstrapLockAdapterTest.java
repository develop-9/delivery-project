package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.delivery_project.user_service.global.crypto.AesGcmCipher;

/**
 * pg_advisory_xact_lock 자체가 실제로 동시 요청을 막는지 raw JDBC 커넥션 2개로 검증한다.
 * 어댑터/포트를 거치지 않고 락 키가 실제로 동작하는지만 확인 — 서비스 레이어에서
 * 락 호출 순서가 맞는지는 AuthCommandServiceTest에서 별도로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AesGcmCipher.class)
class PostgresMasterBootstrapLockAdapterTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void 같은_키로_advisory_lock을_먼저_잡은_트랜잭션이_있으면_뒤이은_요청은_풀릴_때까지_대기한다() throws Exception {
		// given
		String lockQuery = "SELECT pg_advisory_xact_lock(hashtextextended('user_service:master_bootstrap', 0))";

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch lockAcquired = new CountDownLatch(1);
		CountDownLatch releaseLock = new CountDownLatch(1);

		try (Connection connectionA = dataSource.getConnection()) {
			connectionA.setAutoCommit(false);

			Future<Boolean> holderTask = executor.submit(() -> {
				try (var statement = connectionA.createStatement()) {
					statement.executeQuery(lockQuery);
					lockAcquired.countDown();
					releaseLock.await(5, TimeUnit.SECONDS);
				}
				return true;
			});

			assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

			// when: 두 번째 커넥션은 짧은 statement_timeout으로 같은 키에 락을 시도한다.
			Future<Boolean> blockedTask = executor.submit(() -> {
				try (Connection connectionB = dataSource.getConnection()) {
					connectionB.setAutoCommit(false);
					try (var setTimeout = connectionB.createStatement()) {
						setTimeout.execute("SET LOCAL statement_timeout = '1000ms'");
					}
					try (var statement = connectionB.createStatement()) {
						statement.executeQuery(lockQuery);
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
		}
	}
}
