package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 재고 <b>쿼리</b> 측 포트. InventoryQueryService 가 쓴다.
 *
 * <p>여기 있는 조회는 상태를 바꾸기 위한 것이 아니라 화면·타 서비스에 보여주기 위한 것이다.
 * 상태 변경·불변식 검사를 위한 조회는 {@link InventoryCommandRepository} 에 있다.
 */
public interface InventoryQueryRepository {

	Optional<Inventory> findDetailById(UUID inventoryId);

	Page<Inventory> search(InventorySearchCondition condition, Pageable pageable);

	/**
	 * 상품 여러 건의 재고를 한 번에 가져온다 (내부 API 배치 조회).
	 *
	 * <p>재고가 없는 상품은 결과에서 조용히 빠진다. 호출하는 쪽(company-service)이
	 * "재고 없음"으로 처리하면 되고, 없다고 예외를 내면 상품 목록 전체가 실패한다.
	 */
	List<Inventory> findAllByProductIds(Collection<UUID> productIds);
}
