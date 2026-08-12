package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 상품 생성 시 초기 재고 레코드를 만드는 내부 요청 (company-service 가 호출).
 *
 * <p><b>상품 ID 만 받는다.</b> 허브·수량·업체를 받지 않는 이유는 각각 다르다.
 * <ul>
 *   <li>허브 — order 가 hub-service 에서 목록을 받아 <b>모든 허브에</b> 하나씩 만든다(8/4 회의)</li>
 *   <li>수량 — 항상 0 으로 시작한다. 호출자가 정하면 입고 이력 없이 재고가 생겨 출처를 못 따진다</li>
 *   <li>업체 — 명세상 company 가 보내지 않는다. {@code p_inventories.company_id} 는 비워둔다</li>
 * </ul>
 */
public record InventoryInternalCreateRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId
) {
}
