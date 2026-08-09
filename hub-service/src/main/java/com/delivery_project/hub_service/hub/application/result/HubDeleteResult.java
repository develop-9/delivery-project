package com.delivery_project.hub_service.hub.application.result;

import java.time.Instant;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * 허브 삭제 응답 (01_hubs.md 5번).
 *
 * <p>{@code deletedHubRouteCount} 는 D6 로 함께 논리 삭제된 이동정보 수다.
 * 출발지든 도착지든 이 허브가 걸린 구간이 전부 포함된다.
 */
public record HubDeleteResult(
		UUID hubId,
		String name,
		int deletedHubRouteCount,
		Instant deletedAt,
		UUID deletedBy
) {
	public static HubDeleteResult from(Hub hub, int deletedHubRouteCount) {
		return new HubDeleteResult(
				hub.getId(),
				hub.getName(),
				deletedHubRouteCount,
				hub.getDeletedAt(),
				hub.getDeletedBy()
		);
	}
}
