package com.delivery_project.hub_service.global.common;

import java.util.UUID;

/**
 * Access Token 을 파싱한 결과. {@code sub} 클레임이 {@code userId}, {@code role} 클레임이 {@code role} 이다.
 *
 * <p>공통 규약(00_common.md)은 소속 허브(`hubId`) 클레임도 적어두었지만 <b>지금 user-service 가
 * 발급하는 토큰에는 없다.</b> hub-service 권한표에서 {@code HUB_MANAGER} 는 허브·이동정보를 읽기만 하고
 * 담당 허브별로 갈리는 동작이 없어, 이 서비스에는 {@code role} 만 있으면 충분하다.
 * 담당 허브로 갈라야 하는 요구가 생기면 그때 user-service 와 클레임을 맞춘다.
 */
public record JwtPrincipal(UUID userId, Role role) {
}
