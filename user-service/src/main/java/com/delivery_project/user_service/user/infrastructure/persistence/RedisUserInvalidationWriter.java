package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Access Token 무효화 시각을 실제로 Redis에 반영하는 컴포넌트. 더 이상 UserInvalidationRepository
 * 포트를 직접 구현하지 않는다 — 그 포트는 이제 아웃박스(UserInvalidationRepositoryImpl)가 구현하고,
 * 이 클래스는 RabbitMQ 컨슈머가 아웃박스에 쌓인 요청을 처리할 때 호출하는 실제 Redis 쓰기 담당이다.
 */
@Repository
@RequiredArgsConstructor
public class RedisUserInvalidationWriter {

	private static final String KEY_PREFIX = "user:";
	private static final String KEY_SUFFIX = ":invalidatedAt";

	private final StringRedisTemplate redisTemplate;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpirationMillis;

	public void write(UUID userId, Instant invalidatedAt) {
		// Access Token 만료 시간이 지나면 이 키가 있든 없든 어차피 그 이전에 발급된 토큰은
		// 자연 만료되므로, TTL을 access-token-expiration만큼만 잡아 Redis에 무한히 쌓이지 않게 한다.
		Duration ttl = Duration.ofMillis(accessTokenExpirationMillis);
		redisTemplate.opsForValue().set(key(userId), String.valueOf(invalidatedAt.toEpochMilli()), ttl);
	}

	private String key(UUID userId) {
		return KEY_PREFIX + userId + KEY_SUFFIX;
	}
}
