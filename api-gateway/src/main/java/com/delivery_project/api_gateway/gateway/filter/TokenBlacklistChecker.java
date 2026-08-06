package com.delivery_project.api_gateway.gateway.filter;

import reactor.core.publisher.Mono;

/**
 * 사용자 단위 무효화 시각(User Service가 삭제 시 기록)과 토큰 발급 시각을 비교해서
 * 이미 무효화된 토큰인지 확인한다. 저장소 조회 실패 시 fail-open(무효화 아님으로 간주)한다.
 */
public interface TokenBlacklistChecker {

	Mono<Boolean> isRevoked(String userId, long issuedAtMillis);
}
