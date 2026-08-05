package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

	Inventory save(Inventory inventory);

	Optional<Inventory> findById(UUID inventoryId);

	Optional<Inventory> findByProductIdAndHubId(UUID productId, UUID hubId);

	/**
	 * 낙관적 락을 명시적으로 건다.
	 * 조회 시점의 version 을 기억했다가 커밋 때 달라져 있으면 예외.
	 */
	Optional<Inventory> findForUpdateByProductIdAndHubId(UUID productId, UUID hubId);

	boolean existsByProductIdAndHubId(UUID productId, UUID hubId);
}
