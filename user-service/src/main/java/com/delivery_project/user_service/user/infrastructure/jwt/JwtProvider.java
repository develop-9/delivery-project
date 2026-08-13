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
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.global.security.TokenType;
import com.delivery_project.user_service.user.application.port.TokenProvider;
import com.delivery_project.user_service.user.domain.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider implements TokenProvider {

	private static final String ROLE_CLAIM = "role";
	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	private static final String SESSION_ID_CLAIM = "sessionId";
	private static final String BEARER_PREFIX = "Bearer ";

	private final SecretKey accessSecretKey;
	private final SecretKey refreshSecretKey;
	private final long accessTokenExpirationMillis;
	private final long refreshTokenExpirationMillis;

	/**
	 * Access/Refresh Token은 서로 다른 시크릿으로 서명한다. Access Token 시크릿은 앞으로 각 서비스가
	 * 자체 인증 필터를 붙이면 여러 서비스가 알게 되는 반면, Refresh Token은 User Service만 검증하면
	 * 되므로 노출 범위가 훨씬 좁다 — 한쪽 시크릿이 유출돼도 다른 쪽 토큰은 위조할 수 없게 분리한다.
	 */
	public JwtProvider(
			@Value("${jwt.secret}") String accessSecret,
			@Value("${jwt.refresh-secret}") String refreshSecret,
			@Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis,
			@Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis
	) {
		this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
		this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMillis = accessTokenExpirationMillis;
		this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
	}

	@Override
	public String generateAccessToken(UUID userId, Role role, UUID sessionId) {
		Instant now = Instant.now();
		return Jwts.builder()
				// sessionId는 세션(기기) 전체에 걸쳐 고정되므로, refresh 로테이션이 같은 초 안에
				// 일어나면 페이로드가 sessionId만으로는 구분이 안 될 수 있다 — jti로 토큰마다
				// 유일성을 보장한다(재발급 전후 토큰이 항상 실제로 달라야 한다).
				.id(UUID.randomUUID().toString())
				.subject(userId.toString())
				.claim(ROLE_CLAIM, role.name())
				.claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.name())
				.claim(SESSION_ID_CLAIM, sessionId.toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
				.signWith(accessSecretKey)
				.compact();
	}

	@Override
	public String generateRefreshToken(UUID userId, UUID sessionId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(userId.toString())
				.claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH.name())
				.claim(SESSION_ID_CLAIM, sessionId.toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(refreshTokenExpirationMillis)))
				.signWith(refreshSecretKey)
				.compact();
	}

	@Override
	public String resolveToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	@Override
	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationMillis / 1000;
	}

	@Override
	public long getRefreshTokenExpirationMillis() {
		return refreshTokenExpirationMillis;
	}

	@Override
	public JwtPrincipal parseAccessToken(String token) {
		return parse(token, accessSecretKey);
	}

	@Override
	public JwtPrincipal parseRefreshToken(String token) {
		return parse(token, refreshSecretKey);
	}

	/**
	 * 토큰을 검증하고 클레임을 한 번만 파싱해서 userId/role/tokenType/sessionId를 함께 반환한다.
	 * 호출부가 기대하는 종류와 다른 토큰(Access인데 refreshSecretKey로 검증 시도 등)은
	 * 시크릿이 달라 서명 검증 단계에서부터 실패한다.
	 */
	private JwtPrincipal parse(String token, SecretKey key) {
		Claims claims = parseClaims(token, key);
		UUID userId = UUID.fromString(claims.getSubject());
		String roleClaim = claims.get(ROLE_CLAIM, String.class);
		Role role = roleClaim != null ? Role.valueOf(roleClaim) : null;
		String tokenTypeClaim = claims.get(TOKEN_TYPE_CLAIM, String.class);
		TokenType tokenType = tokenTypeClaim != null ? TokenType.valueOf(tokenTypeClaim) : null;
		String sessionIdClaim = claims.get(SESSION_ID_CLAIM, String.class);
		UUID sessionId = sessionIdClaim != null ? UUID.fromString(sessionIdClaim) : null;
		return new JwtPrincipal(userId, role, tokenType, sessionId);
	}

	private Claims parseClaims(String token, SecretKey key) {
		try {
			return Jwts.parser()
					.verifyWith(key)
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
