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
 *
 * <p><b>{@code callerId} 는 담지 않는다.</b> JWT 필터가 파생시킨 인증 컨텍스트이지 클라이언트가 제출한
 * 데이터가 아니라서, Command 에 넣으면 신원을 위조할 수 있는 형태가 된다. 서비스 파라미터로 따로 받는다.
 */
public record HubRouteCreateCommand(
		UUID departureHubId,
		UUID arrivalHubId,
		BigDecimal distanceKm,
		int durationMin
) {
}
