package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 상품 생성 시 초기 재고 레코드를 만드는 내부 요청 (company-service 가 호출).
 *
 * <p>수량과 허브를 받지 않는다. "상품이 생겼다"는 사실만 알리면 order 가 hub-service 에서
 * 허브 목록을 받아 <b>모든 허브에 수량 0 인 행</b>을 하나씩 만든다(8/4 회의 결정).
 *
 * <p>{@code companyId} 는 {@code p_inventories.company_id} 가 NOT NULL 이라 필요하다.
 * order 는 상품이 어느 업체 것인지 알 방법이 없다.
 */
public record InventoryInternalCreateRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId,

		@NotNull(message = "업체 ID는 필수입니다.")
		UUID companyId
) {
}
