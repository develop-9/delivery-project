package com.delivery_project.user_service.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.crypto.AesGcmCipher;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserTableSchemaInitializer.class, AesGcmCipher.class})
class UserTableSchemaInitializerTest {

	@Autowired
	private UserTableSchemaInitializer userTableSchemaInitializer;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void 두_인스턴스가_동시에_CHECK_제약을_보정해도_예외_없이_끝난다() throws Exception {
		// given: 두 인스턴스 모두 DROP까지 끝낸 뒤에야 ADD를 시도하는 정확한 레이스 상황을
		// barrier로 재현한다 — 실제 프로덕션 메서드(dropApprovalStatusCheckConstraint/
		// addApprovalStatusCheckConstraintIfAbsent)를 그대로 호출해서, 테스트가 실제 코드
		// 경로가 아니라 별도로 베낀 SQL을 검증하는 일이 없게 한다.
		String table = "public.p_users";

		CyclicBarrier bothDropped = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Runnable task = () -> {
				userTableSchemaInitializer.dropApprovalStatusCheckConstraint(table);
				await(bothDropped);
				userTableSchemaInitializer.addApprovalStatusCheckConstraintIfAbsent(table);
			};

			Future<?> taskA = executor.submit(task);
			Future<?> taskB = executor.submit(task);

			// when & then
			assertThatCode(() -> {
				taskA.get(5, TimeUnit.SECONDS);
				taskB.get(5, TimeUnit.SECONDS);
			}).doesNotThrowAnyException();
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void run은_최신_ApprovalStatus_값_전체를_허용하는_CHECK_제약을_보정한다() {
		// when
		userTableSchemaInitializer.run(null);

		// then: 다시 실행해도(제약이 이미 있는 상태) 예외 없이 끝난다
		assertThatCode(() -> userTableSchemaInitializer.run(null)).doesNotThrowAnyException();
	}

	private void await(CyclicBarrier barrier) {
		try {
			barrier.await(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
