package com.delivery_project.user_service.user.domain.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 단위 Access Token 무효화 시각을 기록하는 포트. Gateway가 JWT의 iat와 이 값을 비교해서
 * 삭제된 사용자의 토큰을 만료 전에도 차단한다(Gateway JWT 인증 필터 참고).
 */
public interface UserInvalidationRepository {

	void invalidate(UUID userId, Instant invalidatedAt);
}
