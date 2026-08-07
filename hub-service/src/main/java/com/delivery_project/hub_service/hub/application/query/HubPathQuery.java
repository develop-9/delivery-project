package com.delivery_project.hub_service.hub.application.query;

import java.util.UUID;

/**
 * 허브 간 경로 조회 (02_hub-routes.md 11번, 03_internal.md 14번).
 *
 * <p>외부 API 와 내부 API 가 <b>같은 Query 로 같은 서비스 메서드</b>를 부른다.
 * 산출 로직이 갈라지면 화면에 보이는 경로와 실제 배송 경로가 어긋난다.
 */
public record HubPathQuery(
		UUID departureHubId,
		UUID arrivalHubId
) {
}
