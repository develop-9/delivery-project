package com.delivery_project.user_service.user.domain.token;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

/**
 * TokenProvider.parse()의 결과. Refresh Token은 role claim이 없어 role이 null일 수 있다.
 */
public record JwtPrincipal(UUID userId, Role role) {
}
