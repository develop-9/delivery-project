package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface InventoryQueryRepository {

	Optional<Inventory> findDetailById(UUID inventoryId);

	Page<Inventory> search(InventorySearchCondition condition, Pageable pageable);
}
