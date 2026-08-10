package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token은 refresh() 시점에 DB 레벨로 승인 상태를 다시 검증하는 이중 방어가 있어서
 * (AuthCommandService.refresh() 참고), 여기서 Redis 호출이 실패해도 예외를 그대로 던지지 않고
 * fail-open으로 넘어간다 — 서킷이 열려도(또는 Redis가 실제로 죽어도) 로그인/로그아웃 자체가
 * 막히지 않게 하기 위함. UserInvalidationRepository(계정 삭제/정지 무효화)처럼 반드시 나중에
 * 재시도돼야 하는 값이 아니라서 아웃박스 대상은 아니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

	private static final String KEY_PREFIX = "refresh-token:";
	private static final String CIRCUIT_BREAKER_NAME = "redis";

	private final StringRedisTemplate redisTemplate;

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "saveFallback")
	public void save(UUID userId, String refreshToken, Duration ttl) {
		redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
	}

	@SuppressWarnings("unused")
	private void saveFallback(UUID userId, String refreshToken, Duration ttl, Throwable throwable) {
		log.warn("[Redis] Refresh Token 저장 실패(서킷 open 포함) — 로그인은 성공 처리하되 이후 재발급은 재로그인 필요 userId={}",
				userId, throwable);
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "findByUserIdFallback")
	public Optional<String> findByUserId(UUID userId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
	}

	@SuppressWarnings("unused")
	private Optional<String> findByUserIdFallback(UUID userId, Throwable throwable) {
		log.warn("[Redis] Refresh Token 조회 실패(서킷 open 포함) — 없는 것으로 처리해 재로그인 유도 userId={}", userId, throwable);
		return Optional.empty();
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteByUserIdFallback")
	public void deleteByUserId(UUID userId) {
		redisTemplate.delete(key(userId));
	}

	@SuppressWarnings("unused")
	private void deleteByUserIdFallback(UUID userId, Throwable throwable) {
		log.warn("[Redis] Refresh Token 삭제 실패(서킷 open 포함) — TTL 만료로 자연 정리됨 userId={}", userId, throwable);
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteByUserIdOrThrowFallback")
	public void deleteByUserIdOrThrow(UUID userId) {
		redisTemplate.delete(key(userId));
	}

	@SuppressWarnings("unused")
	private void deleteByUserIdOrThrowFallback(UUID userId, Throwable throwable) {
		log.warn("[Redis] 로그아웃 시 Refresh Token 삭제 실패(서킷 open 포함) — 정지/삭제와 달리 승인 상태"
				+ " 재검증으로 방어되지 않는 경로라 실패를 숨기지 않는다 userId={}", userId, throwable);
		throw new IllegalStateException("로그아웃 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", throwable);
	}

	private String key(UUID userId) {
		return KEY_PREFIX + userId;
	}
}
