package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 상품 생성 시 초기 재고 레코드를 만드는 내부 요청 (company-service 가 호출).
 *
 * 외부 {@link InventoryCreateRequest} 와 달리 수량을 받지 않는다.
 * "상품이 생겼다"는 사실만 알리는 것이라 초기 보유 수량은 항상 0 이고,
 * 실제 수량은 입고(inbound) API 로만 올라간다.
 */
public record InventoryInternalCreateRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId,

		@NotNull(message = "허브 ID는 필수입니다.")
		UUID hubId,

		@NotNull(message = "업체 ID는 필수입니다.")
		UUID companyId
) {
}
