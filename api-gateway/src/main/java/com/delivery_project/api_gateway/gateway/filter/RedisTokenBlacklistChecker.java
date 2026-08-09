package com.delivery_project.api_gateway.gateway.filter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklistChecker implements TokenBlacklistChecker {

	private static final String KEY_PREFIX = "user:";
	private static final String KEY_SUFFIX = ":invalidatedAt";

	private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

	@Override
	public Mono<Boolean> isRevoked(String userId, long issuedAtMillis) {
		return reactiveStringRedisTemplate.opsForValue().get(invalidatedAtKey(userId))
				.map(Long::parseLong)
				.defaultIfEmpty(Long.MIN_VALUE)
				.onErrorResume(e -> {
					log.warn("[Gateway] 무효화 여부 확인 중 Redis 조회 실패, fail-open으로 통과 userId={}", userId, e);
					return Mono.just(Long.MIN_VALUE);
				})
				.map(invalidatedAtMillis -> issuedAtMillis < invalidatedAtMillis);
	}

	private String invalidatedAtKey(String userId) {
		return KEY_PREFIX + userId + KEY_SUFFIX;
	}
}
