package com.delivery_project.user_service.user.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.user.application.port.MasterBootstrapLockPort;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresMasterBootstrapLockAdapter implements MasterBootstrapLockPort {

	private final EntityManager entityManager;

	@Override
	public void lock() { // 트랜잭션이 끝나면 자동으로 풀리는 Lock 사용
		entityManager.createNativeQuery("""
				SELECT pg_advisory_xact_lock(
				    hashtextextended('user_service:master_bootstrap', 0)
				)
				""")
				.getSingleResult();
	}
}
