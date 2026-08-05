package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command.OrderItemCommand;
import com.delivery_project.order_service.order.application.command.OrderUpdateCommand;
import com.delivery_project.order_service.order.application.result.OrderResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.OrderRepository;
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
 * 재고 선점 / 배송 생성 / 알림은 연동 단계에서
 * 이 서비스를 감싸는 OrderFacade(트랜잭션 밖)로 붙인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

	private final OrderRepository orderRepository;
	private final OrderSnapshotCommandService orderSnapshotCommandService;

	public OrderResult create(OrderCreateCommand command) {
		validateNoDuplicatedProduct(command.items());

		Order order = Order.builder()
				.supplierCompanyId(command.supplierCompanyId())
				.receiverCompanyId(command.receiverCompanyId())
				.originHubId(command.originHubId())
				.destHubId(command.destHubId())
				.requesterUserId(command.requesterUserId())
				.requestDetails(command.requestDetails())
				.dueAt(command.dueAt())
				.build();

		command.items().forEach(item -> order.addItem(toItem(item)));

		orderRepository.save(order);

		// 같은 트랜잭션 안에서 첫 이력을 남긴다 (sequence = 1)
		orderSnapshotCommandService.capture(order, EventType.ORDER_CREATED, null);

		log.info("[주문] 생성 : [{}] itemCount={} totalQuantity={} totalPrice={}",
				order.getId(), order.getItemCount(), order.getTotalQuantity(), order.getTotalPrice());

		return OrderResult.from(order);
	}

	public OrderResult update(OrderUpdateCommand command) {
		Order order = findActive(command.orderId());
		order.validateModifiable();

		order.updateDetails(command.requestDetails(), command.dueAt());

		if (command.items() != null) {
			// 상품 구성 변경은 배송 생성 전에만 허용한다
			order.validateItemsModifiable();
			validateNoDuplicatedProduct(command.items());
			order.replaceItems(command.items().stream().map(this::toItem).toList());
		}

		orderSnapshotCommandService.capture(order, EventType.ORDER_MODIFIED, "주문 정보 변경");

		log.info("[주문] 수정 : [{}] itemCount={} totalPrice={}",
				order.getId(), order.getItemCount(), order.getTotalPrice());

		return OrderResult.from(order);
	}

	private Order findActive(UUID orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
	}

	private OrderItem toItem(OrderItemCommand command) {
		return OrderItem.builder()
				.productId(command.productId())
				.productName(command.productName())
				.quantity(command.quantity())
				.unitPrice(command.unitPrice())
				.build();
	}

	/** 같은 상품이 여러 줄로 들어오면 집계 금액이 맞아도 재고 선점 단계에서 반드시 깨진다 */
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
