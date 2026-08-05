package com.delivery_project.user_service.user.domain.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	void save(UUID userId, String refreshToken, Duration ttl);

	Optional<String> findByUserId(UUID userId);

	void deleteByUserId(UUID userId);
}
