package com.delivery_project.hub_service.hub.application.command;

import java.math.BigDecimal;

/**
 * 이동정보 수정 (02_hub-routes.md 9번).
 *
 * <p><b>출발·도착 허브는 없다.</b> 두 값이 구간의 정체성이라 바꿀 수 없고, 구간을 옮기려면
 * 삭제 후 재등록한다. 요청 바디에 담겨 와도 무시한다.
 *
 * <p>거리·시간은 보낸 것만 바뀐다. 문서의 "빈 바디 = 지도 API 재계산"은 연동이 없어
 * 지금은 <b>아무것도 바꾸지 않는 요청</b>이 된다. 연동이 들어오면 그때 재계산으로 되살린다.
 */
public record HubRouteUpdateCommand(
		BigDecimal distanceKm,
		Integer durationMin
) {
}
