package com.delivery_project.api_gateway.gateway.filter;

import reactor.core.publisher.Mono;

/**
 * 두 단계로 무효화 여부를 확인한다. 저장소 조회 실패 시 둘 다 fail-open(무효화 아님으로 간주)한다.
 *
 * 1. 사용자 단위 무효화 시각(User Service가 정지/삭제 시 기록)과 토큰 발급 시각을 비교 —
 *    그 사용자의 모든 기기 세션을 한 번에 차단할 때 쓴다.
 * 2. 세션 단위 블랙리스트(User Service가 로그아웃 시 기록) — sessionId가 있는 토큰에 한해,
 *    로그아웃한 기기의 세션 하나만 골라 차단할 때 쓴다. sessionId가 없는 토큰(이 클레임이
 *    추가되기 전에 발급된 토큰)은 이 단계를 건너뛴다.
 */
public interface TokenBlacklistChecker {

	Mono<Boolean> isRevoked(String userId, long issuedAtMillis, String sessionId);
}
