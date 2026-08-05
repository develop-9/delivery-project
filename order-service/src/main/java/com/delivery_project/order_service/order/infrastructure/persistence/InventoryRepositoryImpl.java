package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

	private final SpringDataInventoryRepository springDataInventoryRepository;

	@Override
	public Inventory save(Inventory inventory) {
		return springDataInventoryRepository.save(inventory);
	}

	@Override
	public Optional<Inventory> findById(UUID inventoryId) {
		return springDataInventoryRepository.findByIdAndDeletedAtIsNull(inventoryId);
	}

	@Override
	public Optional<Inventory> findByProductIdAndHubId(UUID productId, UUID hubId) {
		return springDataInventoryRepository.findByProductIdAndHubIdAndDeletedAtIsNull(productId, hubId);
	}

	@Override
	public Optional<Inventory> findForUpdateByProductIdAndHubId(UUID productId, UUID hubId) {
		return springDataInventoryRepository.findWithLockByProductIdAndHubIdAndDeletedAtIsNull(productId, hubId);
	}

	@Override
	public boolean existsByProductIdAndHubId(UUID productId, UUID hubId) {
		return springDataInventoryRepository.existsByProductIdAndHubIdAndDeletedAtIsNull(productId, hubId);
	}
}
