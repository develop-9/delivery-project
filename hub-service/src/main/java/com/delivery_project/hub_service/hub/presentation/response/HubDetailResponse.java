package com.delivery_project.hub_service.hub.presentation.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.result.HubDetailResult;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/** 단건 조회와 목록 원소가 같은 형태다 (01_hubs.md 2·3번). 감사 필드는 담지 않는다. */
public record HubDetailResponse(
		UUID hubId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId,
		String parentHubName
) {
	public static HubDetailResponse from(HubDetailResult result) {
		return new HubDetailResponse(
				result.hubId(),
				result.name(),
				result.address(),
				result.latitude(),
				result.longitude(),
				result.hubType(),
				result.parentHubId(),
				result.parentHubName()
		);
	}
}
