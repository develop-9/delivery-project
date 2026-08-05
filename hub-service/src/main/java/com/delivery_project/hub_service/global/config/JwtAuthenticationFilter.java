package com.delivery_project.hub_service.global.config;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.delivery_project.hub_service.global.common.JwtPrincipal;
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
 * {@code @PreAuthorize} 가 하고, 거절은 {@link JsonAuthenticationEntryPoint} ·
 * {@link JsonAccessDeniedHandler} 가 팀 공통 에러 형식으로 응답한다.
 *
 * <p>principal 로 {@code UUID} 를 넣는다. {@code SecurityAuditorAware} 가
 * {@code authentication.getName()} 을 UUID 로 파싱해 {@code created_by} 를 채우기 때문에
 * 이 자리에 다른 타입을 넣으면 감사 필드가 조용히 비어 INSERT 가 NOT NULL 제약에 걸린다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtProvider jwtProvider;

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
			String token = jwtProvider.resolveToken(authorizationHeader);
			JwtPrincipal principal = jwtProvider.parse(token);

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
