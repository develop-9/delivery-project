package com.delivery_project.hub_service.hub.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 이동정보 수정 (02_hub-routes.md 9번).
 *
 * <p>{@code hubRouteId} 는 PathVariable 로 오지만 Command 가 흡수한다 — Service 가 값의 출처를
 * 알 필요가 없어야 한다.
 *
 * <p><b>{@code callerId} 는 담지 않는다.</b> JWT 필터가 파생시킨 인증 컨텍스트이지 클라이언트가 제출한
 * 데이터가 아니라서, Command 에 넣으면 신원을 위조할 수 있는 형태가 된다. 서비스 파라미터로 따로 받는다.
 *
 * <p><b>출발·도착 허브는 없다.</b> 두 값이 구간의 정체성이라 바꿀 수 없고, 구간을 옮기려면
 * 삭제 후 재등록한다. 요청 바디에 담겨 와도 무시한다.
 *
 * <p>거리·시간은 보낸 것만 바뀐다. 문서의 "빈 바디 = 지도 API 재계산"은 연동이 없어
 * 지금은 <b>아무것도 바꾸지 않는 요청</b>이 된다. 연동이 들어오면 그때 재계산으로 되살린다.
 */
public record HubRouteUpdateCommand(
		UUID hubRouteId,
		BigDecimal distanceKm,
		Integer durationMin
) {
}
