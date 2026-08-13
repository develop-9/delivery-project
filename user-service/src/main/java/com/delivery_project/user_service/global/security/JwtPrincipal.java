package com.delivery_project.user_service.global.security;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

/**
 * TokenProvider.parse()의 결과. Refresh Token은 role claim이 없어 role이 null일 수 있다.
 * sessionId는 로그인 시 한 번 발급되어 Access/Refresh Token 쌍이 공유하고, refresh로 토큰이
 * 로테이션돼도 동일하게 유지된다 — 기기/세션 단위로 Refresh Token을 구분하고, 로그아웃 시
 * 그 세션만 골라 무효화하기 위한 식별자다.
 */
public record JwtPrincipal(UUID userId, Role role, TokenType tokenType, UUID sessionId) {
}
