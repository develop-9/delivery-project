package com.delivery_project.hub_service.hub.application.result;

import java.util.List;
import java.util.UUID;

/**
 * 허브 다건 조회 (03_internal.md 13번).
 *
 * <p><b>부분 실패는 에러가 아니다.</b> 일부만 없으면 찾은 것만 {@code hubs} 에 담고 나머지를
 * {@code notFoundHubIds} 로 알려준다. AI 프롬프트 조립처럼 "찾은 것만이라도 쓰는" 호출자가
 * 스스로 판단해야 하기 때문이다. <b>하나도 없을 때만</b> {@code 404 HUB_NOT_FOUND} 다.
 *
 * <p>요청 순서와 응답 순서는 보장하지 않는다. 호출 측이 {@code hubId} 로 매핑해서 쓴다.
 */
public record HubBatchResult(
		List<HubSummaryResult> hubs,
		List<UUID> notFoundHubIds
) {
}
