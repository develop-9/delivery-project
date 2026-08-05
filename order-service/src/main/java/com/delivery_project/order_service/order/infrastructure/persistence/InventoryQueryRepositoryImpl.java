package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryQueryRepository;
import com.delivery_project.order_service.order.domain.repository.InventorySearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryQueryRepositoryImpl implements InventoryQueryRepository {

	private final SpringDataInventoryRepository springDataInventoryRepository;

	@Override
	public Optional<Inventory> findDetailById(UUID inventoryId) {
		return springDataInventoryRepository.findByIdAndDeletedAtIsNull(inventoryId);
	}

	@Override
	public Page<Inventory> search(InventorySearchCondition condition, Pageable pageable) {
		return springDataInventoryRepository.findAll(InventorySpecifications.from(condition), pageable);
	}
}
