package com.delivery_project.hub_service.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.delivery_project.hub_service.global.exception.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * {@code Authorization} 헤더의 Access Token 을 검증해 SecurityContext 를 채운다.
 *
 * <p>토큰이 없거나 유효하지 않으면 인증 정보를 채우지 않고 그대로 통과시킨다.
 * 최종 판단은 {@code SecurityConfig} 의 {@code authorizeHttpRequests} 와
 * {@code @PreAuthorize} 가 하고, 거절은 {@code JsonAuthenticationEntryPoint} ·
 * {@code JsonAccessDeniedHandler} 가 팀 공통 에러 형식으로 응답한다.
 *
 * <p><b>principal 로 {@code JwtPrincipal} 이 아니라 {@code UUID} 를 넣는다.</b>
 * {@code JpaConfig.auditorProvider} 가 principal 이 {@code UUID} 일 때만 그 값을
 * {@code created_by} 로 쓰고 아니면 {@code system.id} 로 떨어지며, 컨트롤러도
 * {@code @AuthenticationPrincipal UUID callerId} 로 받는다. 이 자리에 다른 타입을 넣으면
 * 감사 필드가 조용히 시스템 ID 로 채워지고 {@code callerId} 는 {@code null} 이 된다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtTokenParser jwtTokenParser;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {

		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
			authenticate(authorizationHeader);
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String authorizationHeader) {
		try {
			String token = jwtTokenParser.resolveToken(authorizationHeader);
			JwtPrincipal principal = jwtTokenParser.parse(token);

			var authorities = principal.role() == null
					? List.<SimpleGrantedAuthority>of()
					: List.of(new SimpleGrantedAuthority(ROLE_PREFIX + principal.role().name()));

			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(principal.userId(), null, authorities));
		} catch (BusinessException e) {
			SecurityContextHolder.clearContext();
		}
	}
}
