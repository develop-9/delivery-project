package com.delivery_project.hub_service.hub.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 이동정보 생성 (02_hub-routes.md 6번).
 *
 * <p><b>거리·시간은 필수다.</b> 문서의 D8 은 값을 생략하면 지도 API 가 산출하도록 정하고 있지만
 * 지도 API 연동은 아직 넣지 않았다. 산출할 수단이 없는 상태에서 값을 선택으로 두면
 * 거리·시간이 빈 구간이 생기고, 그 구간을 지나는 경로 조회가 통째로 못 쓰게 된다.
 * 연동이 들어오면 두 필드를 선택으로 되돌린다.
 */
public record HubRouteCreateCommand(
		UUID departureHubId,
		UUID arrivalHubId,
		BigDecimal distanceKm,
		int durationMin
) {
}
