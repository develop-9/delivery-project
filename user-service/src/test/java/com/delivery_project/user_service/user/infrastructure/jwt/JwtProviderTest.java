package com.delivery_project.user_service.user.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.user.domain.entity.Role;

class JwtProviderTest {

	private static final String SECRET = "test-secret-key-for-jwt-provider-unit-test-32bytes-min";

	private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3_600_000L, 1_209_600_000L);

	@Test
	void AccessToken을_파싱하면_userId와_role을_함께_꺼낼_수_있다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		String token = jwtProvider.generateAccessToken(userId, Role.HUB_MANAGER);
		JwtPrincipal principal = jwtProvider.parse(token);

		// then
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isEqualTo(Role.HUB_MANAGER);
	}

	@Test
	void RefreshToken을_파싱하면_role은_null이다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		String token = jwtProvider.generateRefreshToken(userId);
		JwtPrincipal principal = jwtProvider.parse(token);

		// then
		assertThat(principal.userId()).isEqualTo(userId);
		assertThat(principal.role()).isNull();
	}

	@Test
	void 같은_사용자에게_연속으로_발급한_RefreshToken은_서로_달라야_한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		// iat/exp가 초 단위라 같은 초 안에 연속 호출하면 페이로드가 같아질 수 있어서, jti(고유 ID)로 구분되는지 확인
		String token1 = jwtProvider.generateRefreshToken(userId);
		String token2 = jwtProvider.generateRefreshToken(userId);

		// then
		assertThat(token1).isNotEqualTo(token2);
	}

	@Test
	void 같은_사용자에게_연속으로_발급한_AccessToken은_서로_달라야_한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when
		String token1 = jwtProvider.generateAccessToken(userId, Role.COMPANY_MANAGER);
		String token2 = jwtProvider.generateAccessToken(userId, Role.COMPANY_MANAGER);

		// then
		assertThat(token1).isNotEqualTo(token2);
	}

	@Test
	void 유효한_토큰은_parse가_예외를_던지지_않는다() {
		// given
		String token = jwtProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER);

		// when & then
		jwtProvider.parse(token);
	}

	@Test
	void 만료된_토큰은_AUTH_TOKEN_EXPIRED_예외가_발생한다() {
		// given
		JwtProvider expiredTokenProvider = new JwtProvider(SECRET, -1_000L, -1_000L);
		String token = expiredTokenProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER);

		// when & then
		assertThatThrownBy(() -> jwtProvider.parse(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
	}

	@Test
	void 형식이_깨진_토큰은_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		String malformedToken = "not-a-valid-jwt-token";

		// when & then
		assertThatThrownBy(() -> jwtProvider.parse(malformedToken))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void 다른_시크릿으로_서명된_토큰은_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		JwtProvider otherProvider = new JwtProvider("different-secret-key-for-jwt-provider-unit-test-32bytes", 3_600_000L, 1_209_600_000L);
		String token = otherProvider.generateAccessToken(UUID.randomUUID(), Role.MASTER);

		// when & then
		assertThatThrownBy(() -> jwtProvider.parse(token))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}
}
