package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.user.domain.entity.Role;

public interface TokenProvider {

	String generateAccessToken(UUID userId, Role role);

	String generateRefreshToken(UUID userId);

	JwtPrincipal parse(String token);

	long getAccessTokenExpirationSeconds();

	long getRefreshTokenExpirationMillis();
}
