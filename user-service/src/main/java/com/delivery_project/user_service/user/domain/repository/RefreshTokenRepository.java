package com.delivery_project.user_service.user.domain.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	void save(UUID userId, String refreshToken, Duration ttl);

	Optional<String> findByUserId(UUID userId);

	/**
	 * 정지/삭제 시 쓴다. Redis 삭제가 실패해도 예외를 삼키고 넘어간다(fail-open).
	 * 정지/삭제된 사용자는 refresh()가 매번 승인 상태를 재검증해서 막아주므로, 여기서 삭제가 안 돼도 안전하다.
	 */
	void deleteByUserId(UUID userId);

	/**
	 * 로그아웃 시 쓴다. deleteByUserId()와 달리 실패를 삼키지 않고 그대로 던진다.
	 * 로그아웃한 사용자는 여전히 APPROVED 상태라 refresh()의 승인 상태 재검증으로는 방어가 안 되므로,
	 * Redis 삭제 실패를 성공으로 위장하면 안 된다(이미 탈취된 refresh token이 로그아웃 이후에도 계속 유효해짐).
	 */
	void deleteByUserIdOrThrow(UUID userId);
}
