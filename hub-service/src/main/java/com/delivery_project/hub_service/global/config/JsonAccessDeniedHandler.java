package com.delivery_project.hub_service.global.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 인증은 됐지만 권한이 모자랄 때 ({@code 403 AUTH_FORBIDDEN}).
 *
 * <p>{@code @PreAuthorize} 로 걸린 거절은 이 핸들러가 아니라
 * {@code GlobalExceptionHandler} 의 {@code AuthorizationDeniedException} 처리로 간다.
 * 두 경로 모두 같은 코드로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException
	) throws IOException {

		ErrorCode errorCode = ErrorCode.AUTH_FORBIDDEN;

		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.from(errorCode)));
	}
}
