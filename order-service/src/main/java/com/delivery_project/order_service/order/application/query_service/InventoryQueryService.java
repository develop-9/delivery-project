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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

	private final InventoryQueryRepository inventoryQueryRepository;

	/** 내부 배치 조회 상한. 값이 바뀔 때 코드를 안 고치도록 설정으로 뺀다 */
	@Value("${order.internal.inventory-batch-max-size:100}")
	private int maxBatchSize;

	public InventoryResult getInventory(UUID inventoryId) {
		Inventory inventory = inventoryQueryRepository.findDetailById(inventoryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

		log.info("[재고] 단건 조회 : [{}] quantity={} reservedQuantity={} availableQuantity={}",
				inventoryId, inventory.getQuantity(), inventory.getReservedQuantity(),
				inventory.getAvailableQuantity());

		return InventoryResult.from(inventory);
	}

	/**
	 * 상품 여러 건의 재고 배치 조회 (내부 API — company-service 가 상품 목록·검색 시 호출).
	 *
	 * <p>재고가 없는 상품은 결과에서 빠진다. 호출하는 쪽은 "재고 없음"으로 처리하면 되고,
	 * 없다고 예외를 내면 상품 목록 전체가 실패해 버린다.
	 *
	 * <p>요청 개수는 상한으로 막는다. 상한이 없으면 IN 절이 그대로 커져 DB 쪽에서 터진다.
	 */
	public List<InventoryInternalSummaryResult> getInventoriesByProducts(Collection<UUID> productIds) {
		if (productIds.size() > maxBatchSize) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
					String.format("한 번에 조회할 수 있는 상품은 %d개까지입니다. (요청: %d개)",
							maxBatchSize, productIds.size()));
		}

		List<InventoryInternalSummaryResult> results = inventoryQueryRepository.findAllByProductIds(productIds)
				.stream()
				.map(InventoryInternalSummaryResult::from)
				.toList();

		// requested 와 found 가 다른 것이 정상이다. 그 차이가 곧 "재고 미등록 상품 개수"다.
		log.info("[재고] 상품별 배치 조회(내부) : requested={} found={}", productIds.size(), results.size());

		return results;
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
