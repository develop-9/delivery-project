package com.delivery_project.user_service.user.domain.token;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;

public interface TokenProvider {

	String generateAccessToken(UUID userId, Role role);

	String generateRefreshToken(UUID userId);

	JwtPrincipal parse(String token);

	long getAccessTokenExpirationSeconds();

	long getRefreshTokenExpirationMillis();
}
