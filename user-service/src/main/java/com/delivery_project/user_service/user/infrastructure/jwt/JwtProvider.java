package com.delivery_project.user_service.user.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.domain.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey secretKey;
	private final long accessTokenExpirationMillis;
	private final long refreshTokenExpirationMillis;

	public JwtProvider(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis,
			@Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMillis = accessTokenExpirationMillis;
		this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
	}

	public String generateAccessToken(UUID userId, Role role) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.claim(ROLE_CLAIM, role.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
				.signWith(secretKey)
				.compact();
	}

	public String generateRefreshToken(UUID userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(refreshTokenExpirationMillis)))
				.signWith(secretKey)
				.compact();
	}

	public void validateToken(String token) {
		parseClaims(token);
	}

	public UUID getUserId(String token) {
		return UUID.fromString(parseClaims(token).getSubject());
	}

	public Role getRole(String token) {
		String role = parseClaims(token).get(ROLE_CLAIM, String.class);
		return role != null ? Role.valueOf(role) : null;
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (ExpiredJwtException e) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
	}
}
