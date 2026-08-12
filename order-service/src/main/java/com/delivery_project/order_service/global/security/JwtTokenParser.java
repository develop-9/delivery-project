package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 게이트웨이가 relay 한 토큰을 읽는다.
 *
 * <p>서명 검증·만료 확인·블랙리스트 조회는 게이트웨이가 이미 마쳤다. 그래도 여기서 서명을
 * 다시 확인하는 이유는, 게이트웨이를 거치지 않고 서비스 포트로 직접 들어온 요청이
 * 위조 토큰을 들고 있을 수 있어서다.
 */
@Slf4j
@Component
public class JwtTokenParser {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey secretKey;

	public JwtTokenParser(@Value("${jwt.secret}") String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public JwtPrincipal parse(String token) {
		Claims claims = parseClaims(token);

		UUID userId = parseUserId(claims.getSubject());
		Role role = parseRole(claims.get(ROLE_CLAIM, String.class));

		return new JwtPrincipal(userId, role);
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();

		} catch (ExpiredJwtException exception) {
			log.debug("[인증] 만료된 토큰");
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "만료된 토큰입니다.");

		} catch (JwtException | IllegalArgumentException exception) {
			log.debug("[인증] 유효하지 않은 토큰 : {}", exception.getMessage());
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
		}
	}

	private UUID parseUserId(String subject) {
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException | NullPointerException exception) {
			log.warn("[인증] sub 가 UUID 형식이 아니다 : {}", subject);
			throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
		}
	}

	/**
	 * 모르는 역할이면 {@code null} 로 두고 통과시킨다.
	 * 역할이 늘었을 때 order 만 배포가 늦었다고 인증 자체가 막히면 안 된다.
	 * 역할이 필요한 곳에서 권한 부족으로 걸린다.
	 */
	private Role parseRole(String roleClaim) {
		if (roleClaim == null) {
			return null;
		}
		try {
			return Role.valueOf(roleClaim);
		} catch (IllegalArgumentException exception) {
			log.warn("[인증] 알 수 없는 역할 : {}", roleClaim);
			return null;
		}
	}
}
