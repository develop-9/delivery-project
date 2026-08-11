package com.delivery_project.hub_service.hub.application.result;

import java.util.List;
import java.util.UUID;

/**
 * 전체 허브 ID 목록 (03_internal.md 15번).
 *
 * <p>ID 만 담는다. 허브명·좌표가 필요한 호출자는 13번 다건 조회를 쓴다.
 *
 * <p><b>순서를 보장하지 않는다.</b> 호출 측은 집합으로 다룬다.
 */
public record HubIdsResult(
		List<UUID> hubIds
) {
}
