package com.delivery_project.hub_service.hub.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 수정 (01_hubs.md 4번). 전부 선택 항목이고 {@code null} 은 "안 보냄"으로 본다.
 *
 * <p>그래서 <b>"명시적 null 로 지우기"는 표현할 수 없다.</b> 허브에는 지울 수 있는 필드가 없어
 * (이름·주소·좌표 모두 NOT NULL, 상위 허브는 타입이 정한다) 실제로 문제가 되지 않는다.
 */
public record HubUpdateCommand(
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId
) {
}
