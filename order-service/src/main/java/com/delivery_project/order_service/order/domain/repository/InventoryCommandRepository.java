package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.List;
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
	 * 상품의 재고 행을 <b>모두</b> 찾는다. 주문 줄의 inventory_id(선점 대상)를 정할 때 쓴다.
	 *
	 * <p>8/3~8/4 회의에서 재고를 {@code product_id + hub_id} 로 관리하기로 해
	 * 상품 하나에 허브 수만큼 행이 생긴다. 단건({@code Optional})으로 받으면
	 * 여러 행 중 무엇이 뽑혔는지 호출부가 알 수 없어 목록으로 돌려준다.
	 *
	 * <p>어느 허브를 고를지는 호출부 책임이다. 선택 기준은 아직 팀 합의 전이라
	 * {@code OrderCommandService#selectInventory} 의 잠정 규칙을 따른다.
	 *
	 * @return hub_id 오름차순. 없으면 빈 목록
	 */
	List<Inventory> findAllByProductId(UUID productId);

	Optional<Inventory> findByProductIdAndHubId(UUID productId, UUID hubId);

	/**
	 * 낙관적 락을 명시적으로 건다.
	 * 조회 시점의 version 을 기억했다가 커밋 때 달라져 있으면 예외.
	 */
	Optional<Inventory> findForUpdateByProductIdAndHubId(UUID productId, UUID hubId);

	boolean existsByProductIdAndHubId(UUID productId, UUID hubId);
}
