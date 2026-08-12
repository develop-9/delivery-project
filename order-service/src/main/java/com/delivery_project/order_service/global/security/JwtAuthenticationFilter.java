package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.response.ErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 토큰을 읽어 SecurityContext 를 채운다.
 *
 * <p>토큰이 <b>없으면</b> 그냥 통과시킨다. 인증이 필요한지는 SecurityConfig 의 경로 규칙이
 * 판단할 일이고, 필터가 미리 막으면 {@code permitAll} 경로까지 닫힌다.
 * 토큰이 <b>있는데 잘못됐으면</b> 그 자리에서 401 을 돌려준다 — 위조 토큰을 들고 온 요청을
 * 익명으로 취급해 통과시키면 안 된다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenParser jwtTokenParser;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String token = resolveToken(request);

		if (token != null) {
			try {
				setAuthentication(jwtTokenParser.parse(token));
			} catch (BusinessException exception) {
				writeErrorResponse(response, exception.getErrorCode());
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length());
	}

	private void setAuthentication(JwtPrincipal principal) {
		List<GrantedAuthority> authorities = principal.role() != null
				? List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
				: List.of();

		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, authorities);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode)
			throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.from(errorCode)));
	}
}
