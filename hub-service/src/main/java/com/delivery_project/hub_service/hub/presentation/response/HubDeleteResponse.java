package com.delivery_project.hub_service.hub.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubDeleteResult;

public record HubDeleteResponse(
		UUID hubId,
		String name,
		int deletedHubRouteCount,
		Instant deletedAt,
		UUID deletedBy
) {
	public static HubDeleteResponse from(HubDeleteResult result) {
		return new HubDeleteResponse(
				result.hubId(),
				result.name(),
				result.deletedHubRouteCount(),
				result.deletedAt(),
				result.deletedBy()
		);
	}
}
