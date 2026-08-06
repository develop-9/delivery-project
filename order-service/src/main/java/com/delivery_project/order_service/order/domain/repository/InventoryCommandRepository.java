package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.Optional;
import java.util.UUID;

/**
 * 재고 <b>커맨드</b> 측 포트. InventoryCommandService/OrderCommandService 가 쓴다.
 *
 * <p>여기 있는 조회는 화면에 보여주기 위한 것이 아니라 상태를 바꾸기 위해 애그리거트를 불러오거나
 * 불변식을 검사하는 용도다. 목록·검색은 {@link InventoryQueryRepository} 에 있다.
 */
public interface InventoryCommandRepository {

	Inventory save(Inventory inventory);

	Optional<Inventory> findById(UUID inventoryId);

	/**
	 * 상품으로 재고 행을 찾는다.
	 * 팀문서 p_inventories 는 product_id 가 UNIQUE 라 상품 하나에 재고 행도 하나다.
	 * 주문 줄의 inventory_id(선점 대상)를 정할 때 쓴다.
	 */
	Optional<Inventory> findByProductId(UUID productId);

	Optional<Inventory> findByProductIdAndHubId(UUID productId, UUID hubId);

	/**
	 * 낙관적 락을 명시적으로 건다.
	 * 조회 시점의 version 을 기억했다가 커밋 때 달라져 있으면 예외.
	 */
	Optional<Inventory> findForUpdateByProductIdAndHubId(UUID productId, UUID hubId);

	boolean existsByProductIdAndHubId(UUID productId, UUID hubId);
}
