package com.delivery_project.hub_service.hub.presentation.response.internal;

import java.util.List;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubBatchResult;

/**
 * 허브 다건 조회 응답 (03_internal.md 13번).
 *
 * <p>일부만 없으면 200 에 {@code notFoundHubIds} 를 담아 내려준다. 부분 실패를 에러로 만들지 않는 이유는
 * AI 프롬프트 조립처럼 "찾은 것만이라도 쓰는" 호출자가 스스로 판단해야 하기 때문이다.
 *
 * <p><b>요청 순서와 응답 순서는 보장하지 않는다.</b> 호출 측은 {@code hubId} 를 키로 매핑해서 쓴다.
 */
public record HubBatchResponse(
		List<HubSummaryResponse> hubs,
		List<UUID> notFoundHubIds
) {
	public static HubBatchResponse from(HubBatchResult result) {
		return new HubBatchResponse(
				result.hubs().stream().map(HubSummaryResponse::from).toList(),
				result.notFoundHubIds()
		);
	}
}
