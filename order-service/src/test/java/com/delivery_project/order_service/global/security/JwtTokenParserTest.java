package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 게이트웨이가 relay 한 토큰 파싱 규칙.
 *
 * <p>게이트웨이가 이미 검증했더라도, 게이트웨이를 거치지 않고 서비스 포트로 직접 들어온
 * 위조 토큰이 통과하면 안 된다는 것을 고정한다.
 */
class JwtTokenParserTest {

	private static final String SECRET =
			"test-only-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";

	private final JwtTokenParser parser = new JwtTokenParser(SECRET);

	private String token(String secret, UUID userId, String role, Instant expiry) {
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
				.subject(userId.toString())
				.claim("role", role)
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}

	@Test
	@DisplayName("정상 토큰에서 사용자 ID 와 역할을 뽑는다")
	void parseValidToken() {
		// given
		UUID userId = UUID.randomUUID();
		String token = token(SECRET, userId, "COMPANY_MANAGER", Instant.now().plusSeconds(600));

		// when
		JwtPrincipal principal = parser.parse(token);

		// then
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isEqualTo(Role.COMPANY_MANAGER);
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰은 거부한다")
	void rejectForgedSignature() {
		// given — 게이트웨이를 거치지 않고 직접 들어온 위조 토큰
		String forged = token("another-secret-key-long-enough-for-hmac-sha256-algorithm-x",
				UUID.randomUUID(), "MASTER", Instant.now().plusSeconds(600));

		// when & then
		assertThatThrownBy(() -> parser.parse(forged))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("만료된 토큰은 거부한다")
	void rejectExpiredToken() {
		// given
		String expired = token(SECRET, UUID.randomUUID(), "MASTER", Instant.now().minusSeconds(1));

		// when & then
		assertThatThrownBy(() -> parser.parse(expired))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("만료");
	}

	@Test
	@DisplayName("sub 가 UUID 가 아니면 거부한다")
	void rejectNonUuidSubject() {
		// given
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("not-a-uuid")
				.claim("role", "MASTER")
				.expiration(Date.from(Instant.now().plusSeconds(600)))
				.signWith(key)
				.compact();

		// when & then
		assertThatThrownBy(() -> parser.parse(token))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("모르는 역할이면 역할만 비우고 인증은 통과시킨다")
	void unknownRoleDoesNotBlockAuthentication() {
		// given — 역할이 늘었는데 order 만 배포가 늦은 상황
		UUID userId = UUID.randomUUID();
		String token = token(SECRET, userId, "FUTURE_ROLE", Instant.now().plusSeconds(600));

		// when
		JwtPrincipal principal = parser.parse(token);

		// then — 인증은 되고, 역할이 필요한 곳에서 권한 부족으로 걸린다
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isNull();
	}
}
