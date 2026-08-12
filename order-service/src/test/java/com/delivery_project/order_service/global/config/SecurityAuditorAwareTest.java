package com.delivery_project.order_service.global.config;

import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtAuthenticationFilter는 SecurityContext에 principal 자리로 JwtPrincipal 레코드
 * 전체를 넣는다({@code new UsernamePasswordAuthenticationToken(principal, null, authorities)}).
 * getCurrentAuditor()가 이 실제 형태를 정확히 처리하는지 검증한다.
 */
class SecurityAuditorAwareTest {

	private final UUID systemId = UUID.randomUUID();
	private final SecurityAuditorAware auditorAware = new SecurityAuditorAware(systemId);

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("JwtAuthenticationFilter와 동일한 방식으로 인증되면 요청자 userId를 반환한다")
	void authenticatedUserReturnsRealUserId() {
		UUID userId = UUID.randomUUID();
		JwtPrincipal principal = new JwtPrincipal(userId, Role.MASTER);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MASTER"));

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, authorities)
		);

		assertThat(auditorAware.getCurrentAuditor())
				.contains(userId);
	}

	@Test
	@DisplayName("인증 정보가 없으면 systemId를 반환한다")
	void unauthenticatedReturnsSystemId() {
		assertThat(auditorAware.getCurrentAuditor())
				.contains(systemId);
	}
}
