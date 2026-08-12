package com.delivery_project.user_service.user.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.global.security.TokenType;
import com.delivery_project.user_service.user.domain.entity.Role;

class JwtProviderTest {

	private static final String ACCESS_SECRET = "test-access-secret-key-for-jwt-provider-unit-test-32bytes-min";
	private static final String REFRESH_SECRET = "test-refresh-secret-key-for-jwt-provider-unit-test-32bytes-min";

	private final JwtProvider jwtProvider =
			new JwtProvider(ACCESS_SECRET, REFRESH_SECRET, 3_600_000L, 1_209_600_000L);

	@Test
	void AccessToken을_파싱하면_userId와_role과_sessionId를_함께_꺼낼_수_있다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		// when
		String token = jwtProvider.generateAccessToken(userId, Role.HUB_MANAGER, sessionId);
		JwtPrincipal principal = jwtProvider.parseAccessToken(token);

		// then
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isEqualTo(Role.HUB_MANAGER);
		assertThat(principal.tokenType()).isEqualTo(TokenType.ACCESS);
		assertThat(principal.sessionId()).isEqualTo(sessionId);
	}

	@Test
	void RefreshToken을_파싱하면_role은_null이고_sessionId는_꺼낼_수_있다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		// when
		String token = jwtProvider.generateRefreshToken(userId, sessionId);
		JwtPrincipal principal = jwtProvider.parseRefreshToken(token);

		// then
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isNull();
		assertThat(principal.tokenType()).isEqualTo(TokenType.REFRESH);
		assertThat(principal.sessionId()).isEqualTo(sessionId);
	}

	@Test
	void AccessToken을_parseRefreshToken으로_검증하면_시크릿이_달라_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given: Access Token은 accessSecretKey로 서명되어 있어 refreshSecretKey로는 검증이 안 된다
		String token = jwtProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER, UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void RefreshToken을_parseAccessToken으로_검증하면_시크릿이_달라_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given: Refresh Token은 refreshSecretKey로 서명되어 있어 accessSecretKey로는 검증이 안 된다
		String token = jwtProvider.generateRefreshToken(UUID.randomUUID(), UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void 다른_세션으로_발급한_RefreshToken은_서로_달라야_한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		// iat/exp가 초 단위라 같은 초 안에 연속 호출하면 sessionId가 다르다는 사실로만 구분된다
		// (로그인마다 새 sessionId가 발급되므로, 같은 sessionId로 두 번 발급하는 경우는 refresh
		// 로테이션이고 그때는 애초에 페이로드가 같을 이유가 없다 — 여기선 별도 세션 간 구분만 검증)
		String token1 = jwtProvider.generateRefreshToken(userId, UUID.randomUUID());
		String token2 = jwtProvider.generateRefreshToken(userId, UUID.randomUUID());

		// then
		assertThat(token1).isNotEqualTo(token2);
	}

	@Test
	void 다른_세션으로_발급한_AccessToken은_서로_달라야_한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		String token1 = jwtProvider.generateAccessToken(userId, Role.COMPANY_MANAGER, UUID.randomUUID());
		String token2 = jwtProvider.generateAccessToken(userId, Role.COMPANY_MANAGER, UUID.randomUUID());

		// then
		assertThat(token1).isNotEqualTo(token2);
	}

	@Test
	void 유효한_토큰은_parse가_예외를_던지지_않는다() {
		// given
		String token = jwtProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER, UUID.randomUUID());

		// when & then
		jwtProvider.parseAccessToken(token);
	}

	@Test
	void 만료된_토큰은_AUTH_TOKEN_EXPIRED_예외가_발생한다() {
		// given
		JwtProvider expiredTokenProvider = new JwtProvider(ACCESS_SECRET, REFRESH_SECRET, -1_000L, -1_000L);
		String token = expiredTokenProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER, UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
	}

	@Test
	void 형식이_깨진_토큰은_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		String malformedToken = "not-a-valid-jwt-token";

		// when & then
		assertThatThrownBy(() -> jwtProvider.parseAccessToken(malformedToken))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void 다른_시크릿으로_서명된_토큰은_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		JwtProvider otherProvider = new JwtProvider(
				"different-access-secret-key-for-jwt-provider-unit-test-32bytes",
				"different-refresh-secret-key-for-jwt-provider-unit-test-32bytes",
				3_600_000L, 1_209_600_000L);
		String token = otherProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER, UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}
}
