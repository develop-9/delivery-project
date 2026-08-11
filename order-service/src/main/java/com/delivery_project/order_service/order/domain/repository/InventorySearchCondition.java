package com.delivery_project.order_service.order.domain.repository;

import java.util.UUID;

/**
 * 재고 검색 조건. 채워진 것만 AND 로 묶인다.
 *
 * ex) GET /api/v1/inventories?hubId=...&onlyAvailable=true&maxAvailableQuantity=10&sort=quantity,asc
 */
public record InventorySearchCondition(

        UUID productId,
        UUID hubId,
        UUID companyId,

        Integer minQuantity,
        Integer maxQuantity,

        /** 가용 수량(quantity - reserved) 하한·상한. 품절 임박 재고를 뽑을 때 쓴다 */
        Integer minAvailableQuantity,
        Integer maxAvailableQuantity,

        /** true 면 가용 수량이 1 이상인 재고만 */
        Boolean onlyAvailable,

        /** true 면 선점된 수량이 있는(= 진행 중 주문이 물고 있는) 재고만 */
        Boolean onlyReserved
) {
}
