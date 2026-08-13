package com.delivery_project.api_gateway.gateway.filter;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JjwtTokenValidator implements TokenValidator {

	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	private static final String ACCESS_TOKEN_TYPE = "ACCESS";
	private static final String SESSION_ID_CLAIM = "sessionId";

	private final SecretKey jwtSecretKey;

	@Override
	public ValidatedToken validate(String token) {
		Claims claims;
		try {
			claims = Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			throw new ExpiredTokenException();
		} catch (JwtException | IllegalArgumentException e) {
			throw new InvalidTokenException();
		}

		String userId = claims.getSubject();
		if (userId == null || claims.getIssuedAt() == null) {
			throw new InvalidTokenException();
		}

		// Refresh Token도 같은 secret으로 서명되어 있어 위 검증만으로는 구분이 안 된다.
		// Gateway는 Access Token만 통과시켜야 하므로 tokenType 클레임으로 걸러낸다.
		if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
			throw new InvalidTokenException();
		}

		// 이 배포 이전에 발급된 Access Token은 sessionId 클레임이 없을 수 있다 — null이면
		// TokenBlacklistChecker가 세션 단위 차단을 건너뛰고 사용자 단위 체크만 적용한다.
		String sessionId = claims.get(SESSION_ID_CLAIM, String.class);

		return new ValidatedToken(userId, claims.getIssuedAt().getTime(), sessionId);
	}
}
