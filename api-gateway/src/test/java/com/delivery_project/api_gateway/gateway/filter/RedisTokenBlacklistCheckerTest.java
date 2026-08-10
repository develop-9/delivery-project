package com.delivery_project.api_gateway.gateway.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistCheckerTest {

	@Mock
	private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

	@Mock
	private ReactiveValueOperations<String, String> valueOperations;

	private RedisTokenBlacklistChecker checker;

	@Test
	void 무효화_기록이_없으면_false를_반환한다() {
		// given
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, CircuitBreakerRegistry.ofDefaults());
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(checker.isRevoked("user1", 1000L))
				.expectNext(false)
				.verifyComplete();
	}

	@Test
	void 발급시각이_무효화_시각보다_이전이면_true를_반환한다() {
		// given
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, CircuitBreakerRegistry.ofDefaults());
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.just("2000"));

		// when & then
		StepVerifier.create(checker.isRevoked("user1", 1000L))
				.expectNext(true)
				.verifyComplete();
	}

	@Test
	void 발급시각이_무효화_시각_이후면_false를_반환한다() {
		// given
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, CircuitBreakerRegistry.ofDefaults());
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.just("1000"));

		// when & then
		StepVerifier.create(checker.isRevoked("user1", 2000L))
				.expectNext(false)
				.verifyComplete();
	}

	/**
	 * User Service의 UserInvalidationRepositoryImpl이 쓰는 키와 같은 포맷("user:{id}:invalidatedAt")
	 * 이어야 두 서비스가 실제로 같은 값을 주고받는다 — 별도 Gradle 모듈이라 상수 공유가 안 되므로,
	 * 포맷이 조용히 어긋나는 걸 막기 위해 정확한 문자열을 테스트로 못박아둔다.
	 */
	@Test
	void Redis_조회_키는_user_userId_invalidatedAt_형식이다() {
		// given
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, CircuitBreakerRegistry.ofDefaults());
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("user:user1:invalidatedAt")).thenReturn(Mono.empty());

		// when
		StepVerifier.create(checker.isRevoked("user1", 1000L))
				.expectNext(false)
				.verifyComplete();

		// then
		verify(valueOperations).get("user:user1:invalidatedAt");
	}

	@Test
	void Redis_조회가_실패하면_fail_open으로_false를_반환한다() {
		// given
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, CircuitBreakerRegistry.ofDefaults());
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.error(new RuntimeException("connection refused")));

		// when & then
		StepVerifier.create(checker.isRevoked("user1", 1000L))
				.expectNext(false)
				.verifyComplete();
	}

	@Test
	void 서킷이_열려있으면_Redis_응답과_무관하게_fail_open으로_false를_반환한다() {
		// given: 실패율 100% 설정 + 강제 OPEN 전환으로 "장애가 길어 서킷이 열린" 상태를 재현.
		// Redis가 "무효화됨"(true가 나와야 할 값)으로 응답하더라도, 서킷이 열려 있으면 그
		// 응답을 구독조차 하지 않고 즉시 fail-open으로 넘어가야 한다 — 응답 값으로 직접 검증한다.
		CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
				CircuitBreakerConfig.custom().failureRateThreshold(100).build());
		CircuitBreaker circuitBreaker = registry.circuitBreaker("redis");
		circuitBreaker.transitionToOpenState();
		checker = new RedisTokenBlacklistChecker(reactiveStringRedisTemplate, registry);
		when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(Mono.just("999999999999"));

		// when & then
		StepVerifier.create(checker.isRevoked("user1", 1000L))
				.expectNext(false)
				.verifyComplete();
	}
}
