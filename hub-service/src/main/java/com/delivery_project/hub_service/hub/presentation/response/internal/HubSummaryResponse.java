package com.delivery_project.hub_service.hub.presentation.response.internal;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubSummaryResult;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 내부 API 의 허브 표현 (03_internal.md 12·13번).
 *
 * <p>감사 필드도 {@code parentHubName} 도 없다 — 타 서비스가 쓰는 필드만 내려준다.
 * SUB 여부는 {@code parentHubId != hubId} 로 판정한다 (D1).
 */
public record HubSummaryResponse(
		UUID hubId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId
) {
	public static HubSummaryResponse from(HubSummaryResult result) {
		return new HubSummaryResponse(
				result.hubId(),
				result.name(),
				result.address(),
				result.latitude(),
				result.longitude(),
				result.hubType(),
				result.parentHubId()
		);
	}
}
