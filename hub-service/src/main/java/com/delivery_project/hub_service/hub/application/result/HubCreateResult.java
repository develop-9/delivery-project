package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 생성 응답 (01_hubs.md 1번). 조회 계열과 달리 감사 필드를 포함한다.
 */
public record HubCreateResult(
		UUID hubId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId,
		String parentHubName,
		Instant createdAt,
		Instant updatedAt
) {
	public static HubCreateResult from(Hub hub, String parentHubName) {
		return new HubCreateResult(
				hub.getId(),
				hub.getName(),
				hub.getAddress(),
				hub.getLatitude(),
				hub.getLongitude(),
				hub.getHubType(),
				hub.getParentHubId(),
				parentHubName,
				hub.getCreatedAt(),
				hub.getUpdatedAt()
		);
	}
}
