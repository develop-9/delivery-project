package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.UserContextHolder;
import com.delivery_project.order_service.order.application.command.InventoryAdjustCommand;
import com.delivery_project.order_service.order.application.command.InventoryCreateCommand;
import com.delivery_project.order_service.order.application.command.InventoryInboundCommand;
import com.delivery_project.order_service.order.application.command.InventoryTransferCommand;
import com.delivery_project.order_service.order.application.result.InventoryAdjustResult;
import com.delivery_project.order_service.order.application.result.InventoryDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryInboundResult;
import com.delivery_project.order_service.order.application.result.InventoryResult;
import com.delivery_project.order_service.order.application.result.InventoryTransferResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 재고 등록,입고,보정,이관,삭제.
 *
 * 수량 불변식(가용 범위, 선점 수량 하한 등)은 전부 Inventory 엔티티가 스스로 지킨다.
 * 이 서비스는 어떤 행을 찾아 어떤 메서드를 부를지만 결정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCommandService {

	private final InventoryRepository inventoryRepository;

	/** 재고 등록 — 상품을 특정 허브에 배치한다. 수량 증가가 아니다(그건 입고) */
	public InventoryResult create(InventoryCreateCommand command) {
		if (inventoryRepository.existsByProductIdAndHubId(command.productId(), command.hubId())) {
			throw new BusinessException(ErrorCode.INVENTORY_ALREADY_EXISTS,
					"해당 허브에 이미 등록된 상품입니다. 입고 API를 사용해 주세요.");
		}

		Inventory inventory = inventoryRepository.save(Inventory.builder()
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

	/**
	 * 허브 간 이관.
	 *
	 * 출발 차감과 도착 증가가 반드시 한 트랜잭션이어야 한다. 중간에 끊기면 물량이 증발한다.
	 * 차감은 가용(quantity - reserved) 범위 안에서만 — 선점된 물량까지 빼가면
	 * 진행 중인 주문이 배송 완료 시점에 차감할 재고를 잃는다.
	 */
	public InventoryTransferResult transfer(InventoryTransferCommand command) {
		if (command.fromHubId().equals(command.toHubId())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
					"출발 허브와 도착 허브가 같을 수 없습니다.");
		}

		Inventory from = inventoryRepository
				.findByProductIdAndHubId(command.productId(), command.fromHubId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
						"출발 허브에 해당 상품의 재고가 없습니다."));

		from.transferOut(command.quantity());

		// 도착 허브에 재고 행이 없으면 만들어 준다
		Inventory to = inventoryRepository
				.findByProductIdAndHubId(command.productId(), command.toHubId())
				.orElse(null);

		boolean created = false;
		if (to == null) {
			to = inventoryRepository.save(Inventory.builder()
					.productId(command.productId())
					.hubId(command.toHubId())
					.companyId(from.getCompanyId())
					.quantity(0)
					.build());
			created = true;
		}
		to.transferIn(command.quantity());

		log.info("[재고] 이관 : [{}] {} -> {} quantity={} toCreated={}",
				command.productId(), command.fromHubId(), command.toHubId(), command.quantity(), created);

		return InventoryTransferResult.of(command.productId(), command.quantity(), from, to, created);
	}

	/** 논리 삭제 — 진행 중 주문이 선점한 재고가 있으면 막힌다 */
	public InventoryDeleteResult delete(UUID inventoryId) {
		Inventory inventory = findActive(inventoryId);
		inventory.validateDeletable();
		inventory.delete(UserContextHolder.getRequired().userId());

		log.info("[재고] 삭제 : [{}] remainingQuantity={}",
				inventoryId, inventory.getQuantity());

		return InventoryDeleteResult.from(inventory);
	}

	private Inventory findActive(UUID inventoryId) {
		return inventoryRepository.findById(inventoryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
	}
}
