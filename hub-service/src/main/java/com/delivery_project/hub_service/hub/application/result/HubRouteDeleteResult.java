package com.delivery_project.hub_service.hub.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 이동정보 삭제 응답 (02_hub-routes.md 10번).
 *
 * <p>{@code affectedHubPairCount} 는 이 구간이 사라져 경로 산출이 불가능해지는
 * (출발, 도착) 허브 쌍의 수다. <b>삭제를 막지는 않는다</b> — 영향 범위만 알려주고
 * 판단은 호출자에게 맡긴다.
 */
public record HubRouteDeleteResult(
		UUID hubRouteId,
		String departureHubName,
		String arrivalHubName,
		int affectedHubPairCount,
		Instant deletedAt,
		UUID deletedBy
) {
	public static HubRouteDeleteResult from(HubRoute hubRoute, String departureHubName,
			String arrivalHubName, int affectedHubPairCount) {

		return new HubRouteDeleteResult(
				hubRoute.getId(),
				departureHubName,
				arrivalHubName,
				affectedHubPairCount,
				hubRoute.getDeletedAt(),
				hubRoute.getDeletedBy()
		);
	}
}
