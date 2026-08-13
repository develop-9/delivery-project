package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.user.domain.entity.Role;

public interface TokenProvider {

	String generateAccessToken(UUID userId, Role role, UUID sessionId);

	String generateRefreshToken(UUID userId, UUID sessionId);

	/** Access Token 전용 시크릿으로 검증한다. Refresh Token을 넘기면 서명 검증에서 실패한다. */
	JwtPrincipal parseAccessToken(String token);

	/** Refresh Token 전용 시크릿으로 검증한다. Access Token을 넘기면 서명 검증에서 실패한다. */
	JwtPrincipal parseRefreshToken(String token);

	/** "Bearer " 접두사를 검증하고 벗겨낸 순수 토큰 문자열을 반환한다. */
	String resolveToken(String authorizationHeader);

	long getAccessTokenExpirationSeconds();

	long getRefreshTokenExpirationMillis();
}
