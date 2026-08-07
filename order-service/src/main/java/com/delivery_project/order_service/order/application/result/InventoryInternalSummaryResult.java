package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.UUID;

/**
 * 내부 API 의 재고 표현. 타 서비스가 쓰는 필드만 담는다.
 *
 * <p>감사 필드와 {@code reservedQuantity} 는 빼고 {@code availableQuantity}(= 보유 − 선점)만 준다.
 * 선점은 order 내부 사정이고, 호출하는 쪽이 알아야 하는 건 "지금 주문 가능한 수량"이다.
 * 외부 API 용 {@code InventoryResult} 와 분리한 이유이기도 하다.
 */
public record InventoryInternalSummaryResult(
        UUID inventoryId,
        UUID productId,
        UUID hubId,
        Integer quantity,
        Integer availableQuantity
) {
    public static InventoryInternalSummaryResult from(Inventory inventory) {
        return new InventoryInternalSummaryResult(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getHubId(),
                inventory.getQuantity(),
                inventory.getAvailableQuantity());
    }
}
