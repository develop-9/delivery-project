package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.InventoryResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryQueryRepository;
import com.delivery_project.order_service.order.domain.repository.InventorySearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

	private final InventoryQueryRepository inventoryQueryRepository;

	public InventoryResult getInventory(UUID inventoryId) {
		Inventory inventory = inventoryQueryRepository.findDetailById(inventoryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

		log.info("[재고] 단건 조회 : [{}] quantity={} reservedQuantity={} availableQuantity={}",
				inventoryId, inventory.getQuantity(), inventory.getReservedQuantity(),
				inventory.getAvailableQuantity());

		return InventoryResult.from(inventory);
	}

	public Page<InventoryResult> searchInventories(InventorySearchCondition condition, Pageable pageable) {
		Pageable normalized = PageableUtil.normalize(pageable, PageableUtil.INVENTORY_SORTS);
		Page<Inventory> inventories = inventoryQueryRepository.search(condition, normalized);

		log.info("[재고] 검색 : [productId={}, hubId={}, onlyAvailable={}] page={} size={} totalElements={}",
				condition.productId(), condition.hubId(), condition.onlyAvailable(),
				normalized.getPageNumber(), normalized.getPageSize(), inventories.getTotalElements());

		return inventories.map(InventoryResult::from);
	}
}
