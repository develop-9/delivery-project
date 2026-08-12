package com.delivery_project.order_service.global.config;

import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 감사 필드({@code created_by} / {@code updated_by})에 들어갈 주체 판별.
 *
 * <p>필터가 SecurityContext 에 넣는 principal 타입과 여기서 꺼내는 방식이 어긋나면,
 * <b>인증이 완전히 정상인 요청에서도 매번 시스템 ID 로 떨어진다.</b> 그러면 모든 행의 작성자가
 * 시스템으로 기록돼 누가 만들었는지 추적할 수 없다.
 *
 * <p>컴파일도 되고 다른 테스트도 통과하기 때문에 이 검증이 없으면 드러나지 않는다.
 */
class SecurityAuditorAwareTest {

	private final UUID systemId = UUID.randomUUID();
	private final SecurityAuditorAware auditorAware = new SecurityAuditorAware(systemId);

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	/** JwtAuthenticationFilter 와 같은 방식으로 인증 컨텍스트를 구성한다 */
	private void authenticateAs(JwtPrincipal principal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null,
						List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))));
	}

	@Test
	@DisplayName("인증된 요청은 요청자의 userId 가 감사 주체가 된다")
	void authenticatedRequestUsesCallerId() {
		// given — 필터가 넣는 것과 동일하게 JwtPrincipal 객체를 넣는다
		UUID callerId = UUID.randomUUID();
		authenticateAs(new JwtPrincipal(callerId, Role.MASTER));

		// when & then — 시스템 ID 로 떨어지면 모든 행의 작성자가 시스템으로 기록된다
		assertThat(auditorAware.getCurrentAuditor()).contains(callerId);
	}

	@Test
	@DisplayName("역할을 알 수 없어도 userId 는 그대로 쓴다")
	void unknownRoleStillResolvesCallerId() {
		// given — 모르는 역할이면 파싱 단계에서 role 이 null 이 된다
		UUID callerId = UUID.randomUUID();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(new JwtPrincipal(callerId, null), null, List.of()));

		// when & then — 감사 주체는 역할과 무관하다
		assertThat(auditorAware.getCurrentAuditor()).contains(callerId);
	}

	@Test
	@DisplayName("인증이 없으면 시스템 ID 를 쓴다")
	void unauthenticatedUsesSystemId() {
		// when & then — 내부 API 호출처럼 사용자 토큰이 없는 경우
		assertThat(auditorAware.getCurrentAuditor()).contains(systemId);
	}

	@Test
	@DisplayName("익명 인증도 시스템 ID 를 쓴다")
	void anonymousUsesSystemId() {
		// given
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		// when & then
		assertThat(auditorAware.getCurrentAuditor()).contains(systemId);
	}
}
