package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command.OrderItemCommand;
import com.delivery_project.order_service.order.application.command.OrderUpdateCommand;
import com.delivery_project.order_service.order.application.result.OrderResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.InventoryCommandRepository;
import com.delivery_project.order_service.order.domain.repository.OrderCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 주문 접수·수정.
 *
 * 1단계에서는 어떤 외부 서비스도 호출하지 않는다.
 * 재고 선점(reserve) / 배송 생성 / 알림은 연동 단계에서
 * 이 서비스를 감싸는 OrderFacade(트랜잭션 밖)로 붙인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

	private final OrderCommandRepository orderCommandRepository;
	private final InventoryCommandRepository inventoryCommandRepository;
	private final OrderSnapshotCommandService orderSnapshotCommandService;

	public OrderResult create(OrderCreateCommand command) {
		validateNoDuplicatedProduct(command.items());

		Order order = Order.builder()
				.supplierCompanyId(command.supplierCompanyId())
				.receiverCompanyId(command.receiverCompanyId())
				.receiverUserId(command.receiverUserId())
				.requestDetails(command.requestDetails())
				.build();

		command.items().forEach(item -> order.addItem(toItem(item)));

		orderCommandRepository.save(order);

		// 같은 트랜잭션 안에서 첫 이력을 남긴다 (sequence = 1)
		orderSnapshotCommandService.capture(order, EventType.ORDER_CREATED);

		log.info("[주문] 생성 : [{}] receiverCompanyId={} itemCount={}",
				order.getId(), order.getReceiverCompanyId(), order.getItems().size());

		return OrderResult.from(order);
	}

	public OrderResult update(OrderUpdateCommand command) {
		Order order = findActive(command.orderId());
		order.validateModifiable();

		order.updateDetails(command.requestDetails());

		if (command.items() != null) {
			// 상품 구성 변경은 배송 생성 전에만 허용한다
			order.validateItemsModifiable();
			validateNoDuplicatedProduct(command.items());

			// 새 줄을 선점하기 전에 옛 줄을 먼저 놓는다.
			// 순서가 반대면 같은 재고를 두 번 잡은 상태가 되어 수량이 모자란 것처럼 보인다
			releaseAll(order);

			order.replaceItems(command.items().stream().map(this::toItem).toList());
		}

		orderSnapshotCommandService.capture(order, EventType.ORDER_MODIFIED);

		log.info("[주문] 수정 : [{}] itemCount={}", order.getId(), order.getItems().size());

		return OrderResult.from(order);
	}

	/** 배송 생성이 끝났다. delivery 연동 성공 시 {@code OrderFacade} 가 호출한다 */
	public OrderResult confirm(UUID orderId) {
		Order order = findActive(orderId);
		order.confirm();
		orderSnapshotCommandService.capture(order, EventType.DELIVERY_CONFIRMED);

		log.info("[주문] 확정 : [{}]", orderId);

		return OrderResult.from(order);
	}

	/** 배송 생성이 실패했다. 배송 없는 주문이 PENDING 으로 남지 않게 종착 상태로 내린다 */
	public OrderResult markFailed(UUID orderId) {
		Order order = findActive(orderId);
		order.markFailed();
		releaseAll(order);
		orderSnapshotCommandService.capture(order, EventType.ORDER_FAILED);

		log.info("[주문] 실패 처리 : [{}]", orderId);

		return OrderResult.from(order);
	}

	public OrderResult cancel(UUID orderId) {
		Order order = findActive(orderId);
		order.cancel();
		releaseAll(order);
		orderSnapshotCommandService.capture(order, EventType.ORDER_CANCELED);

		log.info("[주문] 취소 : [{}]", orderId);

		return OrderResult.from(order);
	}

	/**
	 * 배송이 끝났다. 선점을 실물 차감으로 확정한다.
	 * 주문 상태는 CONFIRMED 그대로 두고 이력만 남긴다(팀문서 p_orders.status 정의).
	 */
	public OrderResult complete(UUID orderId) {
		Order order = findActive(orderId);

		order.getItems().forEach(item -> inventoryCommandRepository.findById(item.getInventoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND))
				.confirm(item.getQuantity()));

		orderSnapshotCommandService.capture(order, EventType.ORDER_COMPLETED);

		log.info("[주문] 배송 완료 처리 : [{}] itemCount={}", orderId, order.getItems().size());

		return OrderResult.from(order);
	}

	/**
	 * 주문 줄이 잡고 있던 선점을 모두 되돌린다.
	 *
	 * <p>재고 행이 지워졌더라도 취소는 진행돼야 하므로 없으면 건너뛴다.
	 * 되돌릴 대상이 없는 것이지, 취소를 막을 이유는 아니다.
	 */
	private void releaseAll(Order order) {
		order.getItems().forEach(item -> inventoryCommandRepository.findById(item.getInventoryId())
				.ifPresentOrElse(
						inventory -> inventory.release(item.getQuantity()),
						() -> log.warn("[재고] 선점 복원 대상 없음 : orderId={} inventoryId={}",
								order.getId(), item.getInventoryId())));
	}

	/**
	 * 배송 출발 허브를 구한다. 주문 줄이 선점한 재고가 놓인 허브다.
	 *
	 * <p>⚠️ 줄이 여러 개면 서로 다른 허브의 재고를 선점했을 수 있는데, 배송은 출발지가 하나뿐이다.
	 * 지금은 첫 줄의 허브를 쓰고 나머지가 다르면 경고만 남긴다.
	 * 허브 선택 규칙이 정해지면(→ {@code selectInventory}) 이 갈라짐 자체가 없어져야 한다.
	 */
	@Transactional(readOnly = true)
	public UUID resolveDepartureHubId(UUID orderId) {
		Order order = findActive(orderId);

		List<UUID> hubIds = order.getItems().stream()
				.map(item -> inventoryCommandRepository.findById(item.getInventoryId())
						.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND))
						.getHubId())
				.distinct()
				.toList();

		if (hubIds.size() > 1) {
			log.warn("[주문] 주문 줄이 여러 허브에 걸침 : [{}] hubIds={} 첫 허브를 출발지로 사용",
					orderId, hubIds);
		}

		return hubIds.getFirst();
	}

	private Order findActive(UUID orderId) {
		return orderCommandRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
	}

	/**
	 * 주문 줄이 선점할 재고 행을 붙인다.
	 *
	 * 팀문서 p_order_items.inventory_id 는 NOT NULL 이라 줄을 만들 때 반드시 정해져야 한다.
	 * 실제 수량 선점(reserve)은 재고 연동 단계에서 이 줄을 근거로 수행한다.
	 */
	private OrderItem toItem(OrderItemCommand command) {
		List<Inventory> candidates = inventoryCommandRepository.findAllByProductId(command.productId());
		if (candidates.isEmpty()) {
			throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
					"등록된 재고가 없는 상품은 주문할 수 없습니다.");
		}

		Inventory inventory = selectInventory(candidates, command);

		// 여기서 실제로 수량을 잡는다. 가용 수량이 모자라면 INSUFFICIENT_STOCK 으로 끊긴다.
		// 동시 주문은 Inventory 의 @Version 이 막는다 — 늦게 커밋한 쪽이 충돌로 실패한다.
		inventory.reserve(command.quantity(), command.productId().toString());

		return OrderItem.builder()
				.productId(command.productId())
				.quantity(command.quantity())
				.inventoryId(inventory.getId())
				.build();
	}

	/**
	 * 여러 허브의 재고 행 중 하나를 고른다. <b>여기서 고른 허브가 곧 배송 출발 허브가 된다.</b>
	 *
	 * <p>⚠️ 잠정 규칙이다. 어느 허브에서 출고할지는 delivery 의 출발지 입력값이라
	 * order 혼자 정할 수 없고 팀 합의가 필요하다. 후보안은 아래와 같다.
	 * <ul>
	 *   <li>공급업체 소속 허브 — 물건이 실제로 있는 곳. 경로가 자연스럽다</li>
	 *   <li>가용 수량 최다 — 먼 허브가 뽑혀 배송 경로가 비합리적일 수 있다</li>
	 *   <li>클라이언트 지정 — 주문자가 물류망을 알아야 한다</li>
	 * </ul>
	 *
	 * <p>합의 전까지는 <b>주문 수량을 감당할 수 있는 첫 허브</b>를 고른다.
	 * 감당 가능한 곳이 없으면 첫 행을 붙여 선점 단계에서 수량 부족으로 걸리게 둔다 —
	 * 여기서 미리 막으면 "재고 없음"과 "허브 분산" 문제가 같은 오류로 뭉개진다.
	 * 정렬이 고정돼 있어(hub_id 오름차순) 같은 입력이면 항상 같은 허브가 나온다.
	 */
	private Inventory selectInventory(List<Inventory> candidates, OrderItemCommand command) {
		Inventory selected = candidates.stream()
				.filter(inventory -> inventory.getAvailableQuantity() >= command.quantity())
				.findFirst()
				.orElse(candidates.getFirst());

		if (candidates.size() > 1) {
			log.warn("[주문] 허브 선택 규칙 미확정 상태로 재고 선택 : productId={} candidates={} selectedHubId={}",
					command.productId(), candidates.size(), selected.getHubId());
		}

		return selected;
	}

	/** 같은 상품이 여러 줄로 들어오면 재고 선점 단계에서 반드시 깨진다 */
	private void validateNoDuplicatedProduct(List<OrderItemCommand> items) {
		Set<UUID> seen = new HashSet<>();
		for (OrderItemCommand item : items) {
			if (!seen.add(item.productId())) {
				throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM,
						"같은 상품을 여러 줄로 담을 수 없습니다. 수량을 합쳐서 요청해 주세요.");
			}
		}
	}
}
