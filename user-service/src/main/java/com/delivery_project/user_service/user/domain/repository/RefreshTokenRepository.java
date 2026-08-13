package com.delivery_project.user_service.user.domain.repository;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenRepository {

	/** 로그인 시 새 세션을 만든다. 기존 세션(다른 기기)의 토큰은 건드리지 않는다. */
	void save(UUID userId, UUID sessionId, String refreshToken, Duration ttl);

	/**
	 * 저장된 값이 expectedOldToken과 같을 때만 newToken으로 원자적으로 교체한다(Compare-And-Swap).
	 * 조회와 교체를 애플리케이션 코드에서 따로 하면 그 사이에 다른 재발급 요청이 끼어들 수 있어서,
	 * 같은 세션에서 동시에 두 번 재발급 요청이 들어와도 하나만 성공하도록 Redis 쪽에서 원자적으로 처리한다.
	 * 반환값이 false면 이미 다른 요청이 먼저 로테이션했거나 세션이 만료/존재하지 않는 것이다.
	 */
	boolean compareAndRotate(UUID userId, UUID sessionId, String expectedOldToken, String newToken, Duration ttl);

	/** 그 세션의 Refresh Token이 아직 Redis에 살아있는지 확인한다. */
	boolean existsByUserIdAndSessionId(UUID userId, UUID sessionId);

	/**
	 * 정지/삭제 시 쓴다. 그 사용자의 모든 기기 세션을 한 번에 제거한다. Redis 삭제가 실패해도
	 * 예외를 삼키고 넘어간다(fail-open) — 정지/삭제된 사용자는 refresh()가 매번 승인 상태를
	 * 재검증해서 막아주므로, 여기서 삭제가 안 돼도 안전하다.
	 */
	void deleteAllByUserId(UUID userId);

	/**
	 * 로그아웃 시 쓴다. deleteAllByUserId()와 달리 로그아웃을 요청한 기기의 세션 하나만 제거하고,
	 * 실패를 삼키지 않고 그대로 던진다. 로그아웃한 사용자는 여전히 APPROVED 상태라
	 * refresh()의 승인 상태 재검증으로는 방어가 안 되므로, Redis 삭제 실패를 성공으로 위장하면
	 * 안 된다(이미 탈취된 refresh token이 로그아웃 이후에도 계속 유효해짐).
	 */
	void deleteByUserIdAndSessionIdOrThrow(UUID userId, UUID sessionId);

	/**
	 * 로그아웃한 세션의 Access Token을 자연 만료 전에도 즉시 막기 위해, API Gateway가 요청마다
	 * 확인하는 세션 단위 블랙리스트 키를 남긴다. ttl은 Access Token의 남은 유효시간과 맞춰서,
	 * 그 토큰이 어차피 만료될 시점에 Redis에서도 자동으로 정리되게 한다.
	 * deleteByUserIdAndSessionIdOrThrow()와 같은 이유로 실패를 삼키지 않는다.
	 */
	void blacklistSessionOrThrow(UUID userId, UUID sessionId, Duration ttl);
}
