package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisUserInvalidationRepository implements UserInvalidationRepository {

	private static final String KEY_PREFIX = "user:";
	private static final String KEY_SUFFIX = ":invalidatedAt";

	private final StringRedisTemplate redisTemplate;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpirationMillis;

	@Override
	public void invalidate(UUID userId, Instant invalidatedAt) {
		// Access Token 만료 시간이 지나면 이 키가 있든 없든 어차피 그 이전에 발급된 토큰은
		// 자연 만료되므로, TTL을 access-token-expiration만큼만 잡아 Redis에 무한히 쌓이지 않게 한다.
		Duration ttl = Duration.ofMillis(accessTokenExpirationMillis);
		redisTemplate.opsForValue().set(key(userId), String.valueOf(invalidatedAt.toEpochMilli()), ttl);
	}

	private String key(UUID userId) {
		return KEY_PREFIX + userId + KEY_SUFFIX;
	}
}
