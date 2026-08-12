package com.delivery_project.api_gateway.gateway.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.global.exception.ErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * jjwt 파싱(JjwtTokenValidatorTest)과 Redis 조회(RedisTokenBlacklistCheckerTest)는 각자
 * 별도 테스트로 커버되므로, 여기서는 필터의 오케스트레이션(화이트리스트/에러코드 분기)만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private TokenValidator tokenValidator;

	@Mock
	private TokenBlacklistChecker tokenBlacklistChecker;

	@Mock
	private GatewayErrorResponseWriter errorResponseWriter;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthenticationFilter filterUnderTest() {
		return new JwtAuthenticationFilter(tokenValidator, tokenBlacklistChecker, errorResponseWriter);
	}

	@Test
	void 화이트리스트_경로는_토큰_없이_통과한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/auth/login", null);
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(exchange);
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	void 문서_경로는_접두사_일치로_토큰_없이_통과한다() {
		// given: /docs/user-service/v3/api-docs처럼 뒤에 서비스명이 계속 붙는 경로라
		// WHITELIST_PATHS의 정확히 일치가 아니라 접두사 일치로 처리되는지 확인한다.
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/docs/user-service/v3/api-docs", null);
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(exchange);
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	void swagger_ui_정적_리소스_경로도_토큰_없이_통과한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/swagger-ui/index.html", null);
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(exchange);
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	void Authorization_헤더가_없으면_AUTH_TOKEN_INVALID로_응답한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", null);
		when(errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_INVALID)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(errorResponseWriter).write(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		verify(tokenValidator, never()).validate(any());
		verify(chain, never()).filter(any());
	}

	@Test
	void 토큰_검증에서_ExpiredTokenException이면_AUTH_TOKEN_EXPIRED로_응답한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", "expired-token");
		when(tokenValidator.validate("expired-token")).thenThrow(new ExpiredTokenException());
		when(errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_EXPIRED)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(errorResponseWriter).write(exchange, ErrorCode.AUTH_TOKEN_EXPIRED);
		verify(chain, never()).filter(any());
	}

	@Test
	void 토큰_검증에서_InvalidTokenException이면_AUTH_TOKEN_INVALID로_응답한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", "garbage-token");
		when(tokenValidator.validate("garbage-token")).thenThrow(new InvalidTokenException());
		when(errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_INVALID)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(errorResponseWriter).write(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		verify(chain, never()).filter(any());
	}

	@Test
	void 무효화되지_않았으면_통과한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", "valid-token");
		when(tokenValidator.validate("valid-token")).thenReturn(new ValidatedToken("user1", 1000L, "session1"));
		when(tokenBlacklistChecker.isRevoked("user1", 1000L, "session1")).thenReturn(Mono.just(false));
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain, times(1)).filter(exchange);
	}

	@Test
	void 무효화됐으면_AUTH_TOKEN_REVOKED로_응답한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", "revoked-token");
		when(tokenValidator.validate("revoked-token")).thenReturn(new ValidatedToken("user1", 1000L, "session1"));
		when(tokenBlacklistChecker.isRevoked("user1", 1000L, "session1")).thenReturn(Mono.just(true));
		when(errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_REVOKED)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(errorResponseWriter).write(eq(exchange), eq(ErrorCode.AUTH_TOKEN_REVOKED));
		verify(chain, never()).filter(any());
	}

	private ServerWebExchange exchangeFor(String path, String bearerToken) {
		MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get(path);
		if (bearerToken != null) {
			requestBuilder.header("Authorization", "Bearer " + bearerToken);
		}
		return MockServerWebExchange.from(requestBuilder.build());
	}
}
