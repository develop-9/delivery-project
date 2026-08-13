package com.delivery_project.hub_service.hub.application.query;

import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 목록·검색 (01_hubs.md 3번). 모든 조건이 {@code null} 가능하며 {@code null} 인 조건은 무시한다.
 *
 * <p>{@code parentHubId} 는 D1 자기참조 때문에 <b>중앙 허브 자신도 포함</b>한다.
 * 자신을 빼려면 {@code hubType = SUB} 를 함께 준다.
 *
 * <p>페이징은 여기 담지 않는다 — 검색 조건이 아니라
 * {@link com.delivery_project.hub_service.global.util.PageableFactory} 가 맡는 별개 관심사다.
 */
public record HubSearchQuery(
		String keyword,
		HubType hubType,
		UUID parentHubId
) {
}
