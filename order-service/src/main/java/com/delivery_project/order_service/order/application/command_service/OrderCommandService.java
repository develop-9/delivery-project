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
			order.replaceItems(command.items().stream().map(this::toItem).toList());
		}

		orderSnapshotCommandService.capture(order, EventType.ORDER_MODIFIED);

		log.info("[주문] 수정 : [{}] itemCount={}", order.getId(), order.getItems().size());

		return OrderResult.from(order);
	}

	private Order findActive(UUID orderId) {
		return orderCommandRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
	}

	/**
	 * 주문 줄이 선점할 재고 행을 붙인다.
	 *
	 * 팀문서 p_order_items.inventory_id 는 NOT NULL 이라 줄을 만들 때 반드시 정해져야 한다.
	 * p_inventories 는 product_id 가 UNIQUE 라 상품 하나에 재고 행도 하나다.
	 * 실제 수량 선점(reserve)은 재고 연동 단계에서 이 줄을 근거로 수행한다.
	 */
	private OrderItem toItem(OrderItemCommand command) {
		Inventory inventory = inventoryCommandRepository.findByProductId(command.productId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
						"등록된 재고가 없는 상품은 주문할 수 없습니다."));

		return OrderItem.builder()
				.productId(command.productId())
				.quantity(command.quantity())
				.inventoryId(inventory.getId())
				.build();
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
