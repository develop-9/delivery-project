package com.delivery_project.hub_service.hub.application.query;

import java.util.List;
import java.util.UUID;

/**
 * 내부 허브 다건 조회 (03_internal.md 13번).
 *
 * <p>{@code hubIds} 의 개수 제한(1~17개)은 허브 총수라는 도메인 사실에서 나오므로
 * Bean Validation 이 아니라 Service 가 판정한다.
 */
public record HubBatchQuery(
		List<UUID> hubIds
) {
}
