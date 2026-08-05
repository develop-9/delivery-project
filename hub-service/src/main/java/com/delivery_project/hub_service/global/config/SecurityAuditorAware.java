package com.delivery_project.hub_service.global.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code created_by} / {@code updated_by} 를 채우는 감사자 공급자.
 *
 * <p>공통 파일의 {@code JpaConfig} 는 {@code @EnableJpaAuditing} 뿐이라
 * {@code AuditorAware} 빈이 없으면 {@code @CreatedBy} 가 값을 채우지 않는다.
 * 두 컬럼은 {@code nullable = false} 이므로 이 빈이 없으면 INSERT 가 항상 실패한다.
 *
 * <p>TODO JWT 파싱 필터가 들어오기 전까지 인증 주체가 없어 {@link Optional#empty()} 를 돌려준다.
 * 그동안 저장 경로는 NOT NULL 제약에 걸린다. 클레임명은 user-service 담당자와 확정 후 반영한다.
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
			return Optional.empty();
		}

		return parseUserId(authentication.getName());
	}

	private Optional<UUID> parseUserId(String name) {
		try {
			return Optional.of(UUID.fromString(name));
		} catch (IllegalArgumentException e) {
			log.warn("[Audit] 사용자 ID 가 UUID 형식이 아니다 principal={}", name);
			return Optional.empty();
		}
	}
}
