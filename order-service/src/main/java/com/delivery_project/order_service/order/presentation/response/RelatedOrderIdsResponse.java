package com.delivery_project.order_service.order.presentation.response;

import java.util.List;
import java.util.UUID;

/**
 * 업체 관련 주문 ID 목록 (delivery-service 의 COMPANY_MANAGER 권한 필터용).
 *
 * <p>배열을 그대로 내리지 않고 객체로 감싼다 — 공통 응답의 {@code data} 가 배열이면 나중에 필드를
 * 하나 붙일 때 호출 측 DTO 가 전부 깨진다.
 *
 * <p>0건은 오류가 아니다. 그 업체와 엮인 주문이 아직 없다는 뜻이고, 호출 측은 빈 결과로 다룬다.
 */
public record RelatedOrderIdsResponse(
		List<UUID> orderIds
) {
	public static RelatedOrderIdsResponse from(List<UUID> orderIds) {
		return new RelatedOrderIdsResponse(orderIds);
	}
}
