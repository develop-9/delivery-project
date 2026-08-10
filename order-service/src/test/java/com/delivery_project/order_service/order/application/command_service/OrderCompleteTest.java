package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.InventoryCommandRepository;
import com.delivery_project.order_service.order.domain.repository.OrderCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

/**
 * 배송 완료 통보 처리.
 *
 * <p>delivery 가 통보에 실패해 재시도할 수 있으므로 <b>여러 번 불려도 재고는 한 번만 깎여야</b> 한다.
 * 주문 상태는 완료 후에도 CONFIRMED 그대로라 상태로는 완료 여부를 알 수 없고,
 * 이력에 ORDER_COMPLETED 가 있는지로 판단한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCompleteTest {

	@Mock
	private OrderCommandRepository orderCommandRepository;

	@Mock
	private InventoryCommandRepository inventoryCommandRepository;

	@Mock
	private OrderSnapshotCommandService orderSnapshotCommandService;

	@InjectMocks
	private OrderCommandService orderCommandService;

	private final UUID orderId = UUID.randomUUID();
	private final UUID inventoryId = UUID.randomUUID();

	private Order order;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		inventory = Inventory.builder()
				.productId(UUID.randomUUID())
				.hubId(UUID.randomUUID())
				.companyId(UUID.randomUUID())
				.quantity(100)
				.build();
		inventory.reserve(50, "오징어");

		order = Order.builder()
				.supplierCompanyId(UUID.randomUUID())
				.receiverCompanyId(UUID.randomUUID())
				.receiverUserId(UUID.randomUUID())
				.requestDetails("오전 중 배송 부탁드립니다")
				.build();
		order.addItem(OrderItem.builder()
				.productId(UUID.randomUUID())
				.quantity(50)
				.inventoryId(inventoryId)
				.build());

		given(orderCommandRepository.findById(orderId)).willReturn(Optional.of(order));
		given(inventoryCommandRepository.findById(inventoryId)).willReturn(Optional.of(inventory));
	}

	@Test
	@DisplayName("완료 통보를 받으면 선점분이 실물 차감으로 확정된다")
	void completeConfirmsReservation() {
		// given — 배송 생성까지 끝난 주문
		order.confirm();
		given(orderSnapshotCommandService.alreadyCaptured(orderId, EventType.ORDER_COMPLETED))
				.willReturn(false);

		// when
		orderCommandService.complete(orderId);

		// then
		assertThat(inventory.getQuantity()).isEqualTo(50);
		assertThat(inventory.getReservedQuantity()).isZero();
		then(orderSnapshotCommandService).should().capture(order, EventType.ORDER_COMPLETED);
	}

	@Test
	@DisplayName("같은 통보가 두 번 와도 재고는 한 번만 깎인다")
	void completeIsIdempotent() {
		// given — 이미 완료 이력이 남아 있다 (delivery 의 재시도)
		order.confirm();
		given(orderSnapshotCommandService.alreadyCaptured(orderId, EventType.ORDER_COMPLETED))
				.willReturn(true);

		// when
		orderCommandService.complete(orderId);

		// then — 재고가 그대로여야 한다. 두 번 깎이면 실제보다 적어진다
		assertThat(inventory.getQuantity()).isEqualTo(100);
		assertThat(inventory.getReservedQuantity()).isEqualTo(50);
		then(orderSnapshotCommandService).should(never()).capture(any(), any());
	}

	@Test
	@DisplayName("배송이 생성되지 않은 주문에 완료 통보가 오면 막는다")
	void completeRejectsOrderWithoutDelivery() {
		// given — PENDING. 배송이 아직 없다
		given(orderSnapshotCommandService.alreadyCaptured(orderId, EventType.ORDER_COMPLETED))
				.willReturn(false);

		// when & then
		assertThatThrownBy(() -> orderCommandService.complete(orderId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("완료 처리할 수 없습니다");

		assertThat(inventory.getQuantity()).isEqualTo(100);
	}

	@Test
	@DisplayName("취소된 주문에는 완료 통보를 받지 않는다")
	void completeRejectsCanceledOrder() {
		// given
		order.cancel();
		given(orderSnapshotCommandService.alreadyCaptured(orderId, EventType.ORDER_COMPLETED))
				.willReturn(false);

		// when & then
		assertThatThrownBy(() -> orderCommandService.complete(orderId))
				.isInstanceOf(BusinessException.class);
	}
}
