package com.delivery_project.hub_service.hub.presentation.response.internal;

import java.util.List;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubIdsResult;

/**
 * 전체 허브 ID 목록 응답 (03_internal.md 15번).
 *
 * <p>배열을 그대로 내리지 않고 객체로 감싼다 — 공통 응답의 {@code data} 가 배열이면 나중에 필드를
 * 하나 붙일 때 호출 측이 전부 깨진다.
 *
 * <p><b>순서를 보장하지 않는다.</b> 0건이면 빈 배열이고 그것도 정상 응답이다.
 */
public record HubIdsResponse(
		List<UUID> hubIds
) {
	public static HubIdsResponse from(HubIdsResult result) {
		return new HubIdsResponse(result.hubIds());
	}
}
