package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository
		extends JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {

	Optional<Inventory> findByIdAndDeletedAtIsNull(UUID id);

	/** 상품의 허브별 재고 행 전체. 선택 결과가 실행할 때마다 달라지지 않도록 정렬을 고정한다. */
	List<Inventory> findByProductIdAndDeletedAtIsNullOrderByHubIdAsc(UUID productId);

	Optional<Inventory> findByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

	@Lock(LockModeType.OPTIMISTIC)
	Optional<Inventory> findWithLockByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

	boolean existsByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);
}
