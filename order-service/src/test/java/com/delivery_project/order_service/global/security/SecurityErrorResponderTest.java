package com.delivery_project.order_service.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증 실패(401)와 권한 부족(403)을 나눠 응답하는지.
 *
 * <p>기본 설정으로 두면 <b>토큰이 없을 때도 403</b> 이 나간다. 익명 인증이 켜져 있어
 * {@code authenticated()} 가 인가 예외로 떨어지기 때문이다. 그러면 클라이언트가
 * "로그인이 필요하다"와 "권한이 없다"를 구분할 수 없어 토큰 재발급을 걸지 못한다.
 */
class SecurityErrorResponderTest {

	/** 앱에서는 Boot 가 JavaTimeModule 을 등록해준다. 여기서는 직접 만들어야 Instant 가 직렬화된다 */
	private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

	private final SecurityErrorResponder responder = new SecurityErrorResponder(objectMapper);
	private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("토큰이 없으면 401 로 돌려준다")
	void anonymousGets401() throws IOException {
		// given — 익명 인증이 채워진 상태(토큰 없이 들어온 요청)
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		responder.handle(request, response, new AccessDeniedException("denied"));

		// then
		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("AUTH_UNAUTHORIZED");
	}

	@Test
	@DisplayName("인증은 됐는데 권한이 없으면 403 으로 돌려준다")
	void authenticatedButForbiddenGets403() throws IOException {
		// given
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new JwtPrincipal(UUID.randomUUID(), Role.COMPANY_MANAGER), null,
				List.of(new SimpleGrantedAuthority("ROLE_COMPANY_MANAGER"))));
		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		responder.handle(request, response, new AccessDeniedException("denied"));

		// then
		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("AUTH_FORBIDDEN");
	}

	@Test
	@DisplayName("인증 진입점은 401 을 돌려준다")
	void entryPointGets401() throws IOException {
		// given
		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		responder.commence(request, response, new org.springframework.security.core.AuthenticationException("no auth") {
		});

		// then
		assertThat(response.getStatus()).isEqualTo(401);
	}
}
