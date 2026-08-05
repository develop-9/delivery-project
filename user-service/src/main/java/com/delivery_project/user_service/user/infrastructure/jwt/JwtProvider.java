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
	private static final String BEARER_PREFIX = "Bearer ";

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
				.id(UUID.randomUUID().toString())
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
				.id(UUID.randomUUID().toString())
				.subject(userId.toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(refreshTokenExpirationMillis)))
				.signWith(secretKey)
				.compact();
	}

	public String resolveToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationMillis / 1000;
	}

	public long getRefreshTokenExpirationMillis() {
		return refreshTokenExpirationMillis;
	}

	/**
	 * 토큰을 검증하고 클레임을 한 번만 파싱해서 userId/role을 함께 반환한다.
	 * validateToken()+getUserId()+getRole()을 각각 호출하면 같은 토큰을 여러 번
	 * 파싱/서명검증하게 되어 이 메서드로 통합했다.
	 */
	public JwtPrincipal parse(String token) {
		Claims claims = parseClaims(token);
		UUID userId = UUID.fromString(claims.getSubject());
		String roleClaim = claims.get(ROLE_CLAIM, String.class);
		Role role = roleClaim != null ? Role.valueOf(roleClaim) : null;
		return new JwtPrincipal(userId, role);
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
