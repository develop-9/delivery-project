package com.delivery_project.api_gateway.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JjwtTokenValidatorTest {

	private static final SecretKey SECRET_KEY =
			Keys.hmacShaKeyFor("test-only-secret-key-must-be-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));

	private final JjwtTokenValidator validator = new JjwtTokenValidator(SECRET_KEY);

	@Test
	void 정상_토큰이면_userId와_발급시각을_반환한다() {
		// given
		// JWT의 iat 클레임은 초 단위 정밀도라, 발급 시각을 초 단위로 잘라서 비교해야 한다.
		Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		String token = tokenWith(SECRET_KEY, "11111111-1111-1111-1111-111111111111", issuedAt, issuedAt.plusSeconds(3600));

		// when
		ValidatedToken result = validator.validate(token);

		// then
		assertThat(result.userId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(result.issuedAtMillis()).isEqualTo(issuedAt.toEpochMilli());
	}

	@Test
	void 서명이_다르면_InvalidTokenException을_던진다() {
		// given
		SecretKey otherKey =
				Keys.hmacShaKeyFor("another-secret-key-that-is-also-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();
		String token = tokenWith(otherKey, "11111111-1111-1111-1111-111111111111", now, now.plusSeconds(3600));

		// when & then
		assertThatThrownBy(() -> validator.validate(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void 만료된_토큰이면_ExpiredTokenException을_던진다() {
		// given
		Instant issuedAt = Instant.now().minusSeconds(7200);
		String token = tokenWith(SECRET_KEY, "11111111-1111-1111-1111-111111111111", issuedAt, issuedAt.plusSeconds(3600));

		// when & then
		assertThatThrownBy(() -> validator.validate(token)).isInstanceOf(ExpiredTokenException.class);
	}

	@Test
	void 형식이_깨진_문자열이면_InvalidTokenException을_던진다() {
		assertThatThrownBy(() -> validator.validate("garbage.invalid.token"))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void Refresh_Token은_서명과_만료가_유효해도_InvalidTokenException을_던진다() {
		// given: 같은 secret으로 발급되지만 tokenType이 REFRESH인 토큰(Refresh Token 형태)
		Instant now = Instant.now();
		String token = tokenWith(
				SECRET_KEY, "11111111-1111-1111-1111-111111111111", "REFRESH", now, now.plusSeconds(3600));

		// when & then
		assertThatThrownBy(() -> validator.validate(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenType_클레임이_아예_없으면_InvalidTokenException을_던진다() {
		// given: tokenType 도입 전에 발급된 것처럼 클레임 자체가 없는 토큰
		Instant now = Instant.now();
		String token = tokenWith(SECRET_KEY, "11111111-1111-1111-1111-111111111111", null, now, now.plusSeconds(3600));

		// when & then
		assertThatThrownBy(() -> validator.validate(token)).isInstanceOf(InvalidTokenException.class);
	}

	private String tokenWith(SecretKey key, String subject, Instant issuedAt, Instant expiration) {
		return tokenWith(key, subject, "ACCESS", issuedAt, expiration);
	}

	private String tokenWith(SecretKey key, String subject, String tokenType, Instant issuedAt, Instant expiration) {
		var builder = Jwts.builder()
				.subject(subject)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiration));

		if (tokenType != null) {
			builder.claim("tokenType", tokenType);
		}

		return builder.signWith(key).compact();
	}
}
