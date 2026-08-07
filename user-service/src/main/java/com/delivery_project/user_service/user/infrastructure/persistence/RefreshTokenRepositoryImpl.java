package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

	private static final String KEY_PREFIX = "refresh-token:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void save(UUID userId, String refreshToken, Duration ttl) {
		redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
	}

	@Override
	public Optional<String> findByUserId(UUID userId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
	}

	@Override
	public void deleteByUserId(UUID userId) {
		redisTemplate.delete(key(userId));
	}

	private String key(UUID userId) {
		return KEY_PREFIX + userId;
	}
}
