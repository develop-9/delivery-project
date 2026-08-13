package com.delivery_project.user_service.global.config;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.user.infrastructure.jwt.JwtProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorization 헤더의 Access Token을 검증해 SecurityContext에 인증 정보를 채운다.
 * 토큰이 없거나 유효하지 않으면 인증 정보를 채우지 않고 그대로 통과시키며,
 * 이후 SecurityConfig의 authorizeHttpRequests 규칙(permitAll/authenticated)이 최종 판단한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
			try {
				String token = jwtProvider.resolveToken(authorizationHeader);
				JwtPrincipal principal = jwtProvider.parseAccessToken(token);

				var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
				var authentication = new UsernamePasswordAuthenticationToken(principal.userId(), null, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (BusinessException e) {
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}
}
