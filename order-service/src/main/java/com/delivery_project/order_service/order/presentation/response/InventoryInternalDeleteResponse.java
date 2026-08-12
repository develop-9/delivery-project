package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryInternalDeleteResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 상품 재고 일괄 삭제 응답 (company-service 가 받는다).
 *
 * <p>필드 이름은 company 의 {@code InventoryDeleteFeignResponse} / {@code InventoryDeleteInfo} 에
 * 맞췄다. {@code remainingQuantity} 는 삭제 시점에 남아 있던 보유 수량이다 —
 * 상품을 지웠는데 실물이 남아 있었는지 company 쪽에서 확인할 수 있게 함께 준다.
 */
public record InventoryInternalDeleteResponse(
		List<Item> inventoryList
) {
	public record Item(
			UUID inventoryId,
			Integer remainingQuantity,
			Instant deletedAt
	) {
		public static Item from(InventoryInternalDeleteResult result) {
			return new Item(result.inventoryId(), result.remainingQuantity(), result.deletedAt());
		}
	}

	public static InventoryInternalDeleteResponse from(List<InventoryInternalDeleteResult> results) {
		return new InventoryInternalDeleteResponse(results.stream().map(Item::from).toList());
	}
}
