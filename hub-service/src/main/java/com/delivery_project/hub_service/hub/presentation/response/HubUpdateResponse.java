package com.delivery_project.hub_service.hub.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubUpdateResult;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

public record HubUpdateResponse(
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
	public static HubUpdateResponse from(HubUpdateResult result) {
		return new HubUpdateResponse(
				result.hubId(),
				result.name(),
				result.address(),
				result.latitude(),
				result.longitude(),
				result.hubType(),
				result.parentHubId(),
				result.parentHubName(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
