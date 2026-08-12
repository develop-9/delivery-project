package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;

import java.util.List;
import java.util.UUID;

/**
 * 상품별 허브 재고 조회 응답 (company-service 가 받는다).
 *
 * <p>생성·삭제 응답과 같이 {@code inventoryList} 로 감싼다. 상품 하나가 허브 수만큼 행을 갖기
 * 때문에 어느 응답이든 목록이다.
 *
 * <p>{@code reservedQuantity} 는 담지 않는다. 선점은 order 내부 사정이고, 호출하는 쪽이 알아야
 * 하는 것은 "지금 주문 가능한 수량"인 {@code availableQuantity} 다.
 */
public record InventoryInternalListResponse(
		List<Item> inventoryList
) {
	public record Item(
			UUID inventoryId,
			UUID hubId,
			Integer quantity,
			Integer availableQuantity
	) {
		public static Item from(InventoryInternalSummaryResult result) {
			return new Item(result.inventoryId(), result.hubId(),
					result.quantity(), result.availableQuantity());
		}
	}

	public static InventoryInternalListResponse from(List<InventoryInternalSummaryResult> results) {
		return new InventoryInternalListResponse(results.stream().map(Item::from).toList());
	}
}
