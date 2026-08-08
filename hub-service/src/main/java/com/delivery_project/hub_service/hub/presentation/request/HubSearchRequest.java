package com.delivery_project.hub_service.hub.presentation.request;

import java.util.UUID;

import com.delivery_project.hub_service.hub.application.query.HubSearchQuery;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

import jakarta.validation.constraints.Size;

/**
 * 허브 목록·검색 요청 (01_hubs.md 3번). 세 조건 모두 선택이고 보낸 조건만 걸린다.
 *
 * <p>{@code parentHubId} 는 D1 자기참조 때문에 <b>중앙 허브 자신도 포함</b>한다.
 * 자신을 빼려면 {@code hubType = SUB} 를 함께 준다.
 *
 * <p>{@code keyword} 에 상한을 두는 것은 허브명이 100자라 그보다 긴 검색어는 어떤 행도 맞히지
 * 못하면서 LIKE 스캔 비용만 물리기 때문이다. 주소(255자)가 아니라 허브명에 맞췄다.
 *
 * <p>페이징 파라미터는 여기 담지 않는다 — 검색 조건이 아니라
 * {@link com.delivery_project.hub_service.global.util.PageableFactory} 가 맡는 별개 관심사다.
 */
public record HubSearchRequest(

		@Size(max = 100)
		String keyword,

		HubType hubType,

		UUID parentHubId
) {
	public HubSearchQuery toQuery() {
		return new HubSearchQuery(keyword, hubType, parentHubId);
	}
}
