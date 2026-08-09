package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

}
