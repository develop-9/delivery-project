package com.delivery_project.order_service.global.security;

import java.util.UUID;

/**
 * 인증된 요청 주체. 게이트웨이가 검증한 토큰에서 뽑아낸 값이다.
 *
 * <p>토큰에는 {@code userId} 와 {@code role} 만 들어 있다. 담당 허브·담당 업체는
 * 토큰으로 알 수 없어 그 범위의 권한 검증은 아직 못 한다(delivery-service 도 같은 상태).
 */
public record JwtPrincipal(
		UUID userId,
		Role role
) {
}
