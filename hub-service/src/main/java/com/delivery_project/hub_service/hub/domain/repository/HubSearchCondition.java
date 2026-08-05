package com.delivery_project.hub_service.hub.domain.repository;

import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 목록·검색 조건 (문서 3번). 모든 필드가 {@code null} 가능하며 {@code null} 인 조건은 무시한다.
 *
 * <p>{@code parentHubId} 는 D1 때문에 <b>중앙 허브 자신도 포함</b>한다.
 * 자신을 빼려면 {@code hubType = SUB} 를 함께 준다.
 *
 * @param keyword 허브명·주소 부분 일치
 */
public record HubSearchCondition(
		String keyword,
		HubType hubType,
		UUID parentHubId
) {
}
