package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.InventoryAdjustCommand;
import com.delivery_project.order_service.order.application.command.InventoryCreateCommand;
import com.delivery_project.order_service.order.application.command.InventoryDeleteCommand;
import com.delivery_project.order_service.order.application.command.InventoryInboundCommand;
import com.delivery_project.order_service.order.application.result.InventoryAdjustResult;
import com.delivery_project.order_service.order.application.result.InventoryDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryInboundResult;
import com.delivery_project.order_service.order.application.result.InventoryResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 재고 등록 · 입고 · 보정 · 삭제.
 *
 * 수량 불변식(가용 범위, 선점 수량 하한 등)은 전부 Inventory 엔티티가 스스로 지킨다.
 * 이 서비스는 어떤 행을 찾아 어떤 메서드를 부를지만 결정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCommandService {

	private final InventoryCommandRepository inventoryCommandRepository;

	/** 재고 등록 — 상품을 특정 허브에 배치한다. 수량 증가가 아니다(그건 입고) */
	public InventoryResult create(InventoryCreateCommand command) {
		if (inventoryCommandRepository.existsByProductIdAndHubId(command.productId(), command.hubId())) {
			throw new BusinessException(ErrorCode.INVENTORY_ALREADY_EXISTS,
					"해당 허브에 이미 등록된 상품입니다. 입고 API를 사용해 주세요.");
		}

		Inventory inventory = inventoryCommandRepository.save(Inventory.builder()
				.productId(command.productId())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.quantity(command.quantity())
				.build());

		log.info("[재고] 등록 : [{}] productId={} hubId={} quantity={}",
				inventory.getId(), inventory.getProductId(), inventory.getHubId(), inventory.getQuantity());

		return InventoryResult.from(inventory);
	}

	/** 입고 — 보유 수량에 누적한다 */
	public InventoryInboundResult inbound(InventoryInboundCommand command) {
		Inventory inventory = findActive(command.inventoryId());
		int previousQuantity = inventory.getQuantity();

		inventory.inbound(command.quantity());

		log.info("[재고] 입고 : [{}] {} -> {} note={}",
				command.inventoryId(), previousQuantity, inventory.getQuantity(), command.note());

		return InventoryInboundResult.of(inventory, command.quantity(), previousQuantity);
	}

	/** 실사 보정 — 덮어쓰기. 선점 수량 아래로는 못 내린다 */
	public InventoryAdjustResult adjust(InventoryAdjustCommand command) {
		Inventory inventory = findActive(command.inventoryId());
		int previousQuantity = inventory.getQuantity();

		inventory.adjust(command.quantity(), command.reason());

		log.info("[재고] 보정 : [{}] {} -> {} reason={}",
				command.inventoryId(), previousQuantity, inventory.getQuantity(), command.reason());

		return InventoryAdjustResult.of(inventory, previousQuantity, command.reason());
	}

	/** 논리 삭제 — 진행 중 주문이 선점한 재고가 있으면 막힌다 */
	public InventoryDeleteResult delete(InventoryDeleteCommand command) {
		Inventory inventory = findActive(command.inventoryId());
		inventory.validateDeletable();
		inventory.delete(command.deletedBy());

		log.info("[재고] 삭제 : [{}] remainingQuantity={}",
				command.inventoryId(), inventory.getQuantity());

		return InventoryDeleteResult.from(inventory);
	}

	private Inventory findActive(UUID inventoryId) {
		return inventoryCommandRepository.findById(inventoryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
	}
}
