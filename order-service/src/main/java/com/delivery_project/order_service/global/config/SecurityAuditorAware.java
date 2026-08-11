package com.delivery_project.order_service.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * {@code created_by} / {@code updated_by} 를 채우는 감사자 공급자.
 *
 * <p>공통 파일의 {@code JpaConfig} 는 {@code @EnableJpaAuditing} 뿐이라
 * {@code AuditorAware} 빈이 없으면 {@code @CreatedBy} 가 값을 채우지 않는다.
 * 두 컬럼은 {@code nullable = false} 이므로 이 빈이 없으면 INSERT 가 항상 실패한다.
 *
 * <p>인증 주체가 없으면 팀 공통 시스템 주체 ID 로 대체한다. 값은 루트 {@code .env} 의
 * {@code SYSTEM_ID} → {@code application.yaml} 의 {@code system.id} 로 주입받는다
 * (user-service 와 동일). 전 서비스가 같은 값을 써야 해서 코드에 박지 않는다.
 */
@Slf4j
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

	private final UUID systemId;

	public SecurityAuditorAware(@Value("${system.id}") UUID systemId) {
		this.systemId = systemId;
	}

	@Override
	public Optional<UUID> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.of(systemId);
		}

		return Optional.of(parseUserId(authentication.getName()));
	}

	private UUID parseUserId(String name) {
		try {
			return UUID.fromString(name);
		} catch (IllegalArgumentException e) {
			log.warn("[Audit] 사용자 ID 가 UUID 형식이 아니다 principal={}", name);
			return systemId;
		}
	}
}
