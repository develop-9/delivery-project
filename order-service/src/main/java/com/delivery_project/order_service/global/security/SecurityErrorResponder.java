package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.response.ErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증·인가 실패를 팀 공통 오류 응답으로 돌려준다.
 *
 * <p>둘을 나누는 것이 핵심이다. 기본 설정으로 두면 <b>토큰이 없을 때도 403</b> 이 나가는데,
 * 그러면 클라이언트가 "로그인이 필요하다"와 "권한이 없다"를 구분할 수 없다.
 * 토큰이 만료돼 재발급을 걸어야 하는 상황이 그냥 차단으로 읽힌다.
 *
 * <ul>
 *   <li>인증 안 됨(토큰 없음·익명) → <b>401</b></li>
 *   <li>인증됐지만 권한 부족 → <b>403</b></li>
 * </ul>
 *
 * <p>Security 필터에서 나는 예외는 {@code GlobalExceptionHandler} 를 타지 않는다.
 * DispatcherServlet 앞에서 끊기기 때문이다. 그래서 응답 형식을 여기서 따로 맞춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	/** 인증 자체가 안 된 요청 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		log.debug("[인증] 인증되지 않은 요청 : {} {}", request.getMethod(), request.getRequestURI());
		write(response, ErrorCode.AUTH_UNAUTHORIZED);
	}

	/**
	 * 인증은 됐지만 권한이 모자란 요청.
	 *
	 * <p>토큰이 없는 요청도 여기로 온다. 익명 인증이 켜져 있어 {@code AnonymousAuthenticationToken}
	 * 이 채워지고, 그러면 {@code authenticated()} 가 <b>인증 예외가 아니라 인가 예외</b>로 떨어지기
	 * 때문이다. 그대로 두면 토큰이 없을 때도 403 이 나가므로 익명이면 401 로 돌려보낸다.
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException exception) throws IOException {
		if (isAnonymous()) {
			log.debug("[인증] 인증되지 않은 요청 : {} {}", request.getMethod(), request.getRequestURI());
			write(response, ErrorCode.AUTH_UNAUTHORIZED);
			return;
		}

		log.warn("[인가] 권한 없는 요청 : {} {}", request.getMethod(), request.getRequestURI());
		write(response, ErrorCode.AUTH_FORBIDDEN);
	}

	private boolean isAnonymous() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null || authentication instanceof AnonymousAuthenticationToken;
	}

	private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.from(errorCode)));
	}
}
