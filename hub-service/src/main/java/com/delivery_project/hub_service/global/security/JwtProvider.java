package com.delivery_project.hub_service.global.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.delivery_project.hub_service.global.security.JwtPrincipal;
import com.delivery_project.hub_service.global.security.Role;
import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 토큰 <b>검증·파싱만</b> 한다. 발급은 user-service 몫이라 generate 계열 메서드가 없다.
 *
 * <p>게이트웨이가 이미 검증한 토큰을 relay 하지만 여기서 서명을 다시 확인한다.
 * {@code X-} 헤더를 쓰지 않아 서비스는 요청이 게이트웨이를 거쳤는지 알 수 없고,
 * 검증을 생략하면 서명 없는 토큰으로도 MASTER 를 자칭할 수 있기 때문이다.
 *
 * <p>{@code jwt.secret} 은 user-service 와 같은 값이어야 한다. 다르면 서명 검증이 전부 실패해
 * 모든 요청이 401 이 된다.
 */
@Component
public class JwtProvider {

	private static final String ROLE_CLAIM = "role";
	private static final String BEARER_PREFIX = "Bearer ";

	private final SecretKey secretKey;

	/**
	 * 키가 비어 있으면 여기서 기동을 멈춘다.
	 *
	 * <p>{@code .env} 에 {@code JWT_SECRET=} 만 적어두는 실수가 흔한데, 그냥 두면
	 * {@code Keys.hmacShaKeyFor} 가 내는 메시지만으로는 원인을 찾기 어렵다.
	 * 인증이 전부 401 로 무너지는 것보다 뜨지 않는 편이 낫다.
	 */
	public JwtProvider(@Value("${jwt.secret}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"jwt.secret 이 비어 있다. 루트 .env 의 JWT_SECRET 을 user-service 와 같은 값으로 채운다");
		}

		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String resolveToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	/**
	 * 서명을 검증하고 클레임을 한 번만 읽어 {@code userId} · {@code role} 을 함께 돌려준다.
	 *
	 * <p>Refresh Token 에는 {@code role} 클레임이 없어 {@code role} 이 {@code null} 일 수 있다.
	 * hub-service 는 Refresh Token 을 받을 일이 없지만, 잘못 온 토큰이 권한 없는 사용자로
	 * 취급되도록 예외 대신 {@code null} 을 둔다.
	 */
	public JwtPrincipal parse(String token) {
		Claims claims = parseClaims(token);
		UUID userId = UUID.fromString(claims.getSubject());
		String roleClaim = claims.get(ROLE_CLAIM, String.class);

		return new JwtPrincipal(userId, roleClaim == null ? null : toRole(roleClaim));
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
		}
	}

	/** user-service 가 hub-service 가 모르는 권한을 새로 추가해도 500 대신 401 이 되게 한다. */
	private Role toRole(String roleClaim) {
		try {
			return Role.valueOf(roleClaim);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
		}
	}
}
