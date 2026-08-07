package com.delivery_project.hub_service.hub.application.result;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 단건·목록 조회 응답 (01_hubs.md 2·3번).
 *
 * <p><b>감사 필드가 없다.</b> 허브는 고정 17개를 시드로 한 번에 넣어 생성 시각이 전부 같고
 * 수정도 사실상 없어 화면에 쓸 정보가 아니다. 생성·수정 응답에만 담는다.
 */
public record HubDetailResult(
		UUID hubId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId,
		String parentHubName
) {
	public static HubDetailResult from(Hub hub, String parentHubName) {
		return new HubDetailResult(
				hub.getId(),
				hub.getName(),
				hub.getAddress(),
				hub.getLatitude(),
				hub.getLongitude(),
				hub.getHubType(),
				hub.getParentHubId(),
				parentHubName
		);
	}
}
