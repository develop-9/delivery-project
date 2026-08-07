package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 내부 API 의 허브 표현 (03_internal.md 12·13번).
 *
 * <p>타 서비스가 실제로 쓰는 필드만 담는다 — 감사 필드도, {@code parentHubName} 도 없다.
 * 호출 측은 {@code parentHubId != hubId} 로 {@code SUB} 여부를 판정할 수 있다 (D1).
 */
public record HubSummaryResult(
		UUID hubId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId
) {
	public static HubSummaryResult from(Hub hub) {
		return new HubSummaryResult(
				hub.getId(),
				hub.getName(),
				hub.getAddress(),
				hub.getLatitude(),
				hub.getLongitude(),
				hub.getHubType(),
				hub.getParentHubId()
		);
	}
}
