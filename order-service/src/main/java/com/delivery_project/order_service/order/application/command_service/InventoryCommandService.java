package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.InventoryAdjustCommand;
import com.delivery_project.order_service.order.application.command.InventoryCreateCommand;
import com.delivery_project.order_service.order.application.command.InventoryDeleteCommand;
import com.delivery_project.order_service.order.application.command.InventoryInboundCommand;
import com.delivery_project.order_service.order.application.command.InventoryInternalCreateCommand;
import com.delivery_project.order_service.order.application.port.HubPort;
import com.delivery_project.order_service.order.application.result.InventoryInternalDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryAdjustResult;
import com.delivery_project.order_service.order.application.result.InventoryDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryInboundResult;
import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;
import com.delivery_project.order_service.order.application.result.InventoryResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
	private final HubPort hubPort;

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

	/**
	 * 초기 재고 레코드 생성 (내부 API — company-service 가 상품 생성 시 호출).
	 *
	 * 등록 규칙(같은 상품·허브 중복 금지)은 외부 API 와 같아야 하므로 {@link #create} 를 그대로 탄다.
	 * DTO 는 분리하되 규칙은 한 곳에 둔다.
	 *
	 * 여기서 나가는 예외가 곧 호출한 쪽의 롤백 신호다 — company 는 이 호출이 실패하면
	 * 상품 생성 자체를 롤백한다(팀문서 서비스 간 호출 표).
	 */
	public List<InventoryInternalSummaryResult> createInitial(InventoryInternalCreateCommand command) {
		List<UUID> hubIds = hubPort.getAllHubIds();

		if (hubIds.isEmpty()) {
			// 허브가 없으면 만들 재고도 없다. 상품 등록까지 막을 이유는 아니라 빈 목록을 돌려준다
			log.warn("[재고] 허브가 없어 초기 재고를 만들지 않는다 : productId={}", command.productId());
			return List.of();
		}

		List<InventoryInternalSummaryResult> created = hubIds.stream()
				.filter(hubId -> notAlreadyRegistered(command.productId(), hubId))
				.map(hubId -> toSummary(create(command.toCreateCommand(hubId))))
				.toList();

		log.info("[재고] 초기 레코드 생성(내부) : productId={} hubCount={} createdCount={}",
				command.productId(), hubIds.size(), created.size());

		return created;
	}

	/**
	 * 상품의 재고를 허브 구분 없이 모두 지운다 (company 가 상품을 삭제할 때 호출).
	 *
	 * <p>선점된 재고가 있으면 {@link Inventory#validateDeletable()} 이 막는다. 진행 중인 주문이
	 * 잡고 있는 물량을 지우면 그 주문이 배송될 때 차감할 대상이 사라진다.
	 *
	 * <p>지울 것이 없어도 오류가 아니다. 재고가 만들어지기 전에 상품이 지워졌을 수 있다.
	 */
	public List<InventoryInternalDeleteResult> deleteByProduct(UUID productId, UUID deletedBy) {
		List<Inventory> inventories = inventoryCommandRepository.findAllByProductId(productId);

		List<InventoryInternalDeleteResult> deleted = inventories.stream()
				.peek(Inventory::validateDeletable)
				.peek(inventory -> inventory.delete(deletedBy))
				.map(InventoryInternalDeleteResult::from)
				.toList();

		log.info("[재고] 상품 재고 일괄 삭제(내부) : productId={} deletedCount={}",
				productId, deleted.size());

		return deleted;
	}

	/**
	 * 이미 있는 허브는 건너뛴다.
	 *
	 * <p>company 가 같은 상품으로 다시 호출하면(재시도 등) 중복 등록 예외가 나면서 앞서 만든
	 * 행까지 롤백된다. 건너뛰면 빠진 허브만 채워지고 재호출이 안전해진다.
	 */
	private boolean notAlreadyRegistered(UUID productId, UUID hubId) {
		boolean exists = inventoryCommandRepository.existsByProductIdAndHubId(productId, hubId);

		if (exists) {
			log.debug("[재고] 이미 등록된 허브라 건너뛴다 : productId={} hubId={}", productId, hubId);
		}
		return !exists;
	}

	private InventoryInternalSummaryResult toSummary(InventoryResult created) {
		return new InventoryInternalSummaryResult(
				created.inventoryId(),
				created.productId(),
				created.hubId(),
				created.quantity(),
				created.availableQuantity(),
				created.createdAt());
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
