package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 상품 생성 시 초기 재고 레코드를 만드는 내부 요청 (company-service 가 호출).
 *
 * 외부 {@link InventoryCreateRequest} 와 달리 수량을 받지 않는다.
 * "상품이 생겼다"는 사실만 알리는 것이라 초기 보유 수량은 항상 0 이고,
 * 실제 수량은 입고(inbound) API 로만 올라간다.
 *
 * <p>{@code hubId} 는 상품을 보관하는 허브다. company-service 의 {@code p_products} 에
 * 허브 컬럼이 없어, 호출하는 쪽이 <b>상품 소유 업체의 소속 허브</b>({@code p_companies.hub_id})를
 * 넘기기로 했다. order 는 이 값을 그대로 저장하고 허브 실존 여부는 검증하지 않는다 —
 * 팀문서 서비스 간 호출 표에서 허브 존재 확인은 "업체·상품 등록 시" company 책임이다.
 *
 * <p>이 값이 곧 {@code p_inventories.hub_id} 가 되고, 주문 시 배송 출발 허브로 쓰인다.
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
