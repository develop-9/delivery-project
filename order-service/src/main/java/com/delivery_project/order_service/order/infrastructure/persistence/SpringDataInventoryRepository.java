package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository
		extends JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {

	Optional<Inventory> findByIdAndDeletedAtIsNull(UUID id);

	Optional<Inventory> findByProductIdAndDeletedAtIsNull(UUID productId);

	Optional<Inventory> findByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

	@Lock(LockModeType.OPTIMISTIC)
	Optional<Inventory> findWithLockByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

	boolean existsByProductIdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);
}
