package com.delivery_project.api_gateway.gateway.filter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklistChecker implements TokenBlacklistChecker {

	private static final String USER_KEY_PREFIX = "user:";
	private static final String USER_KEY_SUFFIX = ":invalidatedAt";
	private static final String SESSION_KEY_PREFIX = "session:";
	private static final String SESSION_KEY_SUFFIX = ":blacklisted";
	private static final String CIRCUIT_BREAKER_NAME = "redis";

	private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
	private final CircuitBreakerRegistry circuitBreakerRegistry;

	@Override
	public Mono<Boolean> isRevoked(String userId, long issuedAtMillis, String sessionId) {
		Mono<Boolean> userLevelRevoked = isRevokedByUserLevelInvalidation(userId, issuedAtMillis);
		// sessionId가 없는 토큰(이 클레임이 추가되기 전에 발급된 토큰)은 세션 단위 체크를
		// 아예 건너뛰고 사용자 단위 체크 결과만 쓴다.
		if (sessionId == null) {
			return userLevelRevoked;
		}

		Mono<Boolean> sessionLevelRevoked = isRevokedBySessionBlacklist(userId, sessionId);
		return Mono.zip(userLevelRevoked, sessionLevelRevoked, (byUser, bySession) -> byUser || bySession);
	}

	private Mono<Boolean> isRevokedByUserLevelInvalidation(String userId, long issuedAtMillis) {
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

		return reactiveStringRedisTemplate.opsForValue().get(invalidatedAtKey(userId))
				.map(Long::parseLong)
				.defaultIfEmpty(Long.MIN_VALUE)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				// 개별 호출 실패뿐 아니라 서킷이 열려 있을 때(CallNotPermittedException)도 여기로 온다 —
				// 장애가 길어지면 매 요청마다 타임아웃을 기다리지 않고 즉시 fail-open으로 통과시킨다.
				.onErrorResume(e -> {
					log.warn("[Gateway] 사용자 단위 무효화 확인 중 Redis 조회 실패(서킷 open 포함), fail-open으로 통과 userId={}",
							userId, e);
					return Mono.just(Long.MIN_VALUE);
				})
				.map(invalidatedAtMillis -> issuedAtMillis < invalidatedAtMillis);
	}

	private Mono<Boolean> isRevokedBySessionBlacklist(String userId, String sessionId) {
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

		return reactiveStringRedisTemplate.hasKey(sessionBlacklistKey(userId, sessionId))
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.onErrorResume(e -> {
					log.warn("[Gateway] 세션 단위 블랙리스트 확인 중 Redis 조회 실패(서킷 open 포함), fail-open으로 통과"
							+ " userId={} sessionId={}", userId, sessionId, e);
					return Mono.just(false);
				});
	}

	private String invalidatedAtKey(String userId) {
		return USER_KEY_PREFIX + userId + USER_KEY_SUFFIX;
	}

	private String sessionBlacklistKey(String userId, String sessionId) {
		return SESSION_KEY_PREFIX + userId + ":" + sessionId + SESSION_KEY_SUFFIX;
	}
}
