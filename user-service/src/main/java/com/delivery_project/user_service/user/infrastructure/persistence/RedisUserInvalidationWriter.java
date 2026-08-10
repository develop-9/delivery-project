package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Access Token 무효화 시각을 실제로 Redis에 반영하는 컴포넌트. 더 이상 UserInvalidationRepository
 * 포트를 직접 구현하지 않는다 — 그 포트는 이제 아웃박스(UserInvalidationRepositoryImpl)가 구현하고,
 * 이 클래스는 RabbitMQ 컨슈머가 아웃박스에 쌓인 요청을 처리할 때 호출하는 실제 Redis 쓰기 담당이다.
 *
 * RefreshTokenRepositoryImpl/RedisTokenBlacklistChecker와 같은 "redis" 서킷을 쓴다 — Redis
 * 장애 중 컨슈머가 이 메서드를 반복 호출할 때마다 커맨드 타임아웃(2초)을 매번 기다리지 않고
 * 즉시 실패로 넘어가기 위함이다. 다른 두 곳과 달리 fallback에서 예외를 삼키지 않고 그대로
 * 다시 던진다 — 이 실패는 UserInvalidationQueueConsumer의 재시도 로직이 받아서 처리해야
 * 하므로, 여기서 fail-open으로 조용히 넘어가면 안 된다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisUserInvalidationWriter {

	private static final String KEY_PREFIX = "user:";
	private static final String KEY_SUFFIX = ":invalidatedAt";
	private static final String CIRCUIT_BREAKER_NAME = "redis";

	private final StringRedisTemplate redisTemplate;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpirationMillis;

	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "writeFallback")
	public void write(UUID userId, Instant invalidatedAt) {
		// Access Token 만료 시간이 지나면 이 키가 있든 없든 어차피 그 이전에 발급된 토큰은
		// 자연 만료되므로, TTL을 access-token-expiration만큼만 잡아 Redis에 무한히 쌓이지 않게 한다.
		Duration ttl = Duration.ofMillis(accessTokenExpirationMillis);
		redisTemplate.opsForValue().set(key(userId), String.valueOf(invalidatedAt.toEpochMilli()), ttl);
	}

	@SuppressWarnings("unused")
	private void writeFallback(UUID userId, Instant invalidatedAt, Throwable throwable) {
		log.warn("[Redis] 사용자 무효화 기록 실패(서킷 open 포함) — 컨슈머 재시도로 넘긴다 userId={}", userId, throwable);
		throw throwable instanceof RuntimeException runtimeException
				? runtimeException
				: new IllegalStateException("Redis 무효화 기록 실패", throwable);
	}

	private String key(UUID userId) {
		return KEY_PREFIX + userId + KEY_SUFFIX;
	}
}
