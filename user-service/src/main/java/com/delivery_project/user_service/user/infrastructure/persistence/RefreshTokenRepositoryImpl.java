package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 세션(기기) 단위로 키를 분리한다 — refresh-token:{userId}:{sessionId}. 같은 사용자의 다른 기기
 * 세션은 서로 다른 키를 쓰므로 한쪽 재발급이 다른 쪽 세션을 덮어쓰지 않는다.
 *
 * refresh-token-sessions:{userId} SET에 그 사용자의 활성 sessionId를 같이 관리한다.
 * deleteAllByUserId()(정지/삭제)가 "이 사용자의 모든 세션"을 찾는 유일한 방법이라서다.
 * SET의 TTL은 매 저장/로테이션마다 토큰과 같은 값으로 같이 갱신한다 — 그래야 토큰은 아직
 * 살아있는데 SET만 먼저 만료돼 deleteAllByUserId()가 그 세션을 못 찾는 상황이 생기지 않는다.
 *
 * Refresh Token은 refresh() 시점에 DB 레벨로 승인 상태를 다시 검증하는 이중 방어가 있어서
 * (AuthCommandService.refresh() 참고), save/compareAndRotate/deleteAllByUserId에서 Redis
 * 호출이 실패해도 예외를 그대로 던지지 않고 fail-open으로 넘어간다. 다만 로그아웃(단일 세션
 * 삭제·블랙리스트 등록)은 그 방어가 없는 경로라 fail-closed로 예외를 그대로 던진다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

	private static final String TOKEN_KEY_PREFIX = "refresh-token:";
	private static final String SESSIONS_KEY_PREFIX = "refresh-token-sessions:";
	private static final String BLACKLIST_KEY_PREFIX = "session:";
	private static final String BLACKLIST_KEY_SUFFIX = ":blacklisted";
	private static final String CIRCUIT_BREAKER_NAME = "redis";

	private static final RedisScript<Long> SAVE_SCRIPT = new DefaultRedisScript<>("""
			redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
			redis.call('SADD', KEYS[2], ARGV[3])
			redis.call('PEXPIRE', KEYS[2], ARGV[2])
			return 1
			""", Long.class);

	private static final RedisScript<Long> COMPARE_AND_ROTATE_SCRIPT = new DefaultRedisScript<>("""
			if redis.call('GET', KEYS[1]) == ARGV[1] then
			    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
			    redis.call('SADD', KEYS[2], ARGV[4])
			    redis.call('PEXPIRE', KEYS[2], ARGV[3])
			    return 1
			else
			    return 0
			end
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "saveFallback")
	public void save(UUID userId, UUID sessionId, String refreshToken, Duration ttl) {
		redisTemplate.execute(SAVE_SCRIPT,
				List.of(tokenKey(userId, sessionId), sessionsKey(userId)),
				refreshToken, String.valueOf(ttl.toMillis()), sessionId.toString());
	}

	@SuppressWarnings("unused")
	private void saveFallback(UUID userId, UUID sessionId, String refreshToken, Duration ttl, Throwable throwable) {
		log.warn("[Redis] Refresh Token 저장 실패(서킷 open 포함) — 로그인은 성공 처리하되 이후 재발급은 재로그인 필요"
				+ " userId={} sessionId={}", userId, sessionId, throwable);
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "compareAndRotateFallback")
	public boolean compareAndRotate(UUID userId, UUID sessionId, String expectedOldToken, String newToken, Duration ttl) {
		Long result = redisTemplate.execute(COMPARE_AND_ROTATE_SCRIPT,
				List.of(tokenKey(userId, sessionId), sessionsKey(userId)),
				expectedOldToken, newToken, String.valueOf(ttl.toMillis()), sessionId.toString());
		return Long.valueOf(1L).equals(result);
	}

	@SuppressWarnings("unused")
	private boolean compareAndRotateFallback(
			UUID userId, UUID sessionId, String expectedOldToken, String newToken, Duration ttl, Throwable throwable) {
		log.warn("[Redis] Refresh Token 로테이션 실패(서킷 open 포함) — 재로그인 유도 userId={} sessionId={}",
				userId, sessionId, throwable);
		return false;
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "existsByUserIdAndSessionIdFallback")
	public boolean existsByUserIdAndSessionId(UUID userId, UUID sessionId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey(userId, sessionId)));
	}

	@SuppressWarnings("unused")
	private boolean existsByUserIdAndSessionIdFallback(UUID userId, UUID sessionId, Throwable throwable) {
		log.warn("[Redis] 세션 존재 여부 조회 실패(서킷 open 포함) — 없는 것으로 처리 userId={} sessionId={}",
				userId, sessionId, throwable);
		return false;
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteAllByUserIdFallback")
	public void deleteAllByUserId(UUID userId) {
		String sessionsKey = sessionsKey(userId);
		Set<String> sessionIds = redisTemplate.opsForSet().members(sessionsKey);
		if (sessionIds != null && !sessionIds.isEmpty()) {
			List<String> tokenKeys = sessionIds.stream().map(sessionId -> tokenKey(userId, sessionId)).toList();
			redisTemplate.delete(tokenKeys);
		}
		redisTemplate.delete(sessionsKey);
	}

	@SuppressWarnings("unused")
	private void deleteAllByUserIdFallback(UUID userId, Throwable throwable) {
		log.warn("[Redis] 전체 세션 Refresh Token 삭제 실패(서킷 open 포함) — TTL 만료로 자연 정리됨 userId={}",
				userId, throwable);
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteByUserIdAndSessionIdOrThrowFallback")
	public void deleteByUserIdAndSessionIdOrThrow(UUID userId, UUID sessionId) {
		redisTemplate.delete(tokenKey(userId, sessionId));
		redisTemplate.opsForSet().remove(sessionsKey(userId), sessionId.toString());
	}

	@SuppressWarnings("unused")
	private void deleteByUserIdAndSessionIdOrThrowFallback(UUID userId, UUID sessionId, Throwable throwable) {
		log.warn("[Redis] 로그아웃 시 Refresh Token 삭제 실패(서킷 open 포함) — 정지/삭제와 달리 승인 상태"
				+ " 재검증으로 방어되지 않는 경로라 실패를 숨기지 않는다 userId={} sessionId={}", userId, sessionId, throwable);
		throw new IllegalStateException("로그아웃 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", throwable);
	}

	@Override
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "blacklistSessionOrThrowFallback")
	public void blacklistSessionOrThrow(UUID userId, UUID sessionId, Duration ttl) {
		redisTemplate.opsForValue().set(blacklistKey(userId, sessionId), "1", ttl);
	}

	@SuppressWarnings("unused")
	private void blacklistSessionOrThrowFallback(UUID userId, UUID sessionId, Duration ttl, Throwable throwable) {
		log.warn("[Redis] 로그아웃 시 세션 블랙리스트 등록 실패(서킷 open 포함) — 이 세션의 기존 Access Token이"
				+ " 자연 만료 전까지 계속 유효하게 된다 userId={} sessionId={}", userId, sessionId, throwable);
		throw new IllegalStateException("로그아웃 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", throwable);
	}

	private String tokenKey(UUID userId, UUID sessionId) {
		return tokenKey(userId, sessionId.toString());
	}

	private String tokenKey(UUID userId, String sessionId) {
		return TOKEN_KEY_PREFIX + userId + ":" + sessionId;
	}

	private String sessionsKey(UUID userId) {
		return SESSIONS_KEY_PREFIX + userId;
	}

	private String blacklistKey(UUID userId, UUID sessionId) {
		return BLACKLIST_KEY_PREFIX + userId + ":" + sessionId + BLACKLIST_KEY_SUFFIX;
	}
}
