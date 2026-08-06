package com.delivery_project.api_gateway.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	private static final SecretKey SECRET_KEY =
			Keys.hmacShaKeyFor("test-only-secret-key-must-be-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));

	@Mock
	private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

	@Mock
	private ReactiveValueOperations<String, String> valueOperations;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthenticationFilter filterUnderTest() {
		return new JwtAuthenticationFilter(SECRET_KEY, reactiveStringRedisTemplate, new ObjectMapper());
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
	}

	@Test
	void Authorization_헤더가_없으면_401_AUTH_TOKEN_INVALID를_반환한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", null);

		// when
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		// then
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void 서명이_다른_토큰이면_401_AUTH_TOKEN_INVALID를_반환한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		SecretKey otherKey =
				Keys.hmacShaKeyFor("another-secret-key-that-is-also-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("11111111-1111-1111-1111-111111111111")
				.issuedAt(Date.from(Instant.now()))
				.expiration(Date.from(Instant.now().plusSeconds(3600)))
				.signWith(otherKey)
				.compact();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", token);

		// when
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		// then
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void 만료된_토큰이면_401_AUTH_TOKEN_EXPIRED를_반환한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		String token = Jwts.builder()
				.subject("11111111-1111-1111-1111-111111111111")
				.issuedAt(Date.from(Instant.now().minusSeconds(7200)))
				.expiration(Date.from(Instant.now().minusSeconds(3600)))
				.signWith(SECRET_KEY)
				.compact();
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", token);

		// when
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		// then
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void 무효화_기록이_없으면_정상_통과한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		String token = validToken(Instant.now());
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", token);
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.empty());
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain, times(1)).filter(exchange);
	}

	@Test
	void 발급시각이_무효화_시각보다_이전이면_401_AUTH_TOKEN_REVOKED를_반환한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		Instant issuedAt = Instant.now().minusSeconds(60);
		String token = validToken(issuedAt);
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", token);

		long invalidatedAtMillis = Instant.now().toEpochMilli();
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.just(String.valueOf(invalidatedAtMillis)));

		// when
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		// then
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void Redis_조회가_실패하면_fail_open으로_통과한다() {
		// given
		JwtAuthenticationFilter filter = filterUnderTest();
		String token = validToken(Instant.now());
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me", token);
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.error(new RuntimeException("connection refused")));
		when(chain.filter(exchange)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain, times(1)).filter(exchange);
	}

	private String validToken(Instant issuedAt) {
		return Jwts.builder()
				.subject("11111111-1111-1111-1111-111111111111")
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(issuedAt.plusSeconds(3600)))
				.signWith(SECRET_KEY)
				.compact();
	}

	private ServerWebExchange exchangeFor(String path, String bearerToken) {
		MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get(path);
		if (bearerToken != null) {
			requestBuilder.header("Authorization", "Bearer " + bearerToken);
		}
		return MockServerWebExchange.from(requestBuilder.build());
	}
}
