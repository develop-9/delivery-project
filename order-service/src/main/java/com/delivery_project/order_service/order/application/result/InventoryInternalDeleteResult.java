package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.time.Instant;
import java.util.UUID;

/**
 * 상품 재고 일괄 삭제 결과 (company-service 가 상품 삭제 시 받는다).
 *
 * <p>{@code remainingQuantity} 는 삭제 시점에 남아 있던 보유 수량이다.
 * 상품을 지웠는데 실물이 남아 있었는지 호출 측이 알 수 있어야 한다.
 */
public record InventoryInternalDeleteResult(
		UUID inventoryId,
		UUID hubId,
		Integer remainingQuantity,
		Instant deletedAt
) {
	public static InventoryInternalDeleteResult from(Inventory inventory) {
		return new InventoryInternalDeleteResult(
				inventory.getId(),
				inventory.getHubId(),
				inventory.getQuantity(),
				inventory.getDeletedAt());
	}
}
