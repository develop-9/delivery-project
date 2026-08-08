package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 수정 응답 (01_hubs.md 4번). 생성 응답과 같은 형태다.
 */
public record HubUpdateResult(
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
	public static HubUpdateResult from(Hub hub, String parentHubName) {
		return new HubUpdateResult(
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
