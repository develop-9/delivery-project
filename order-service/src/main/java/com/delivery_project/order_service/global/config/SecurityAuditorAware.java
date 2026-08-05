package com.delivery_project.order_service.global.config;

import lombok.extern.slf4j.Slf4j;
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
 * <p>TODO JWT 파싱 필터가 들어오기 전까지는 인증 주체가 없어 {@link SystemUser#ID} 로 대체한다.
 * 필터가 붙으면 이 대체 로직을 제거한다.
 */
@Slf4j
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

	@Override
	public Optional<UUID> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.of(SystemUser.ID);
		}

		return Optional.of(parseUserId(authentication.getName()));
	}

	private UUID parseUserId(String name) {
		try {
			return UUID.fromString(name);
		} catch (IllegalArgumentException e) {
			log.warn("[Audit] 사용자 ID 가 UUID 형식이 아니다 principal={}", name);
			return SystemUser.ID;
		}
	}
}
