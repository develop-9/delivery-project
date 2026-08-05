package com.delivery_project.hub_service.hub.presentation.response;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubRouteDeleteResult;

public record HubRouteDeleteResponse(
		UUID hubRouteId,
		String departureHubName,
		String arrivalHubName,
		int affectedHubPairCount,
		Instant deletedAt,
		UUID deletedBy
) {
	public static HubRouteDeleteResponse from(HubRouteDeleteResult result) {
		return new HubRouteDeleteResponse(
				result.hubRouteId(),
				result.departureHubName(),
				result.arrivalHubName(),
				result.affectedHubPairCount(),
				result.deletedAt(),
				result.deletedBy()
		);
	}
}
