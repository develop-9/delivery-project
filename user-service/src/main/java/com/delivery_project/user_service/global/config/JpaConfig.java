package com.delivery_project.user_service.global.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

	/**
	 * @CreatedBy/@LastModifiedBy(BaseEntity/BaseDeletableEntity)를 채우는 데 쓰인다.
	 * 인증 정보가 없거나(예: 회원가입) principal이 UUID가 아니면(예: 익명 사용자) 빈 값을 반환해
	 * created_by/updated_by가 null로 저장되도록 한다.
	 */
	@Bean
	public AuditorAware<UUID> auditorProvider() {
		return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getPrincipal)
				.filter(UUID.class::isInstance)
				.map(UUID.class::cast);
	}
}
