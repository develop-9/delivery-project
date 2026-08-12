package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;
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

import java.util.List;
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

	/**
	 * 상품의 허브별 재고 전체 (내부 API — company 가 상품 상세에서 재고 현황을 보여줄 때 쓴다).
	 *
	 * <p>페이징하지 않는다. 상품 하나의 행 수는 허브 수로 고정돼 있어 페이지로 나눌 만큼 크지 않다.
	 *
	 * <p>재고가 없어도 오류가 아니다. 재고 행이 만들어지기 전이거나 이미 삭제된 상품일 수 있다.
	 */
	public List<InventoryInternalSummaryResult> getInventoriesByProduct(UUID productId) {
		List<InventoryInternalSummaryResult> inventories =
				inventoryQueryRepository.findAllByProductId(productId).stream()
						.map(InventoryInternalSummaryResult::from)
						.toList();

		log.info("[재고] 상품별 재고 조회(내부) : productId={} count={}", productId, inventories.size());

		return inventories;
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
