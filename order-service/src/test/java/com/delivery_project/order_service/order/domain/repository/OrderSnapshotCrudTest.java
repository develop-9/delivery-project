package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.global.config.JpaConfig;
import com.delivery_project.order_service.global.config.SecurityAuditorAware;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.entity.OrderItemSnapshot;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.entity.OrderStatus;
import com.delivery_project.order_service.order.infrastructure.persistence.OrderCommandRepositoryImpl;
import com.delivery_project.order_service.order.infrastructure.persistence.OrderSnapshotCommandRepositoryImpl;
import com.delivery_project.order_service.order.infrastructure.persistence.OrderSnapshotQueryRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 주문 이력(스냅샷) CRUD — 기록 · 조회 · 필터 · 논리 삭제 */
@DataJpaTest
@Import({JpaConfig.class, SecurityAuditorAware.class, OrderCommandRepositoryImpl.class,
		OrderSnapshotCommandRepositoryImpl.class, OrderSnapshotQueryRepositoryImpl.class})
class OrderSnapshotCrudTest {

	@Autowired
	private OrderCommandRepository orderCommandRepository;

	@Autowired
	private OrderSnapshotCommandRepository orderSnapshotCommandRepository;

	@Autowired
	private OrderSnapshotQueryRepository orderSnapshotQueryRepository;

	private final UUID squidId = UUID.randomUUID();
	private final UUID kelpId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	private Order order;

	@BeforeEach
	void setUp() {
		order = Order.builder()
				.supplierCompanyId(UUID.randomUUID())
				.receiverCompanyId(UUID.randomUUID())
				.receiverUserId(userId)
				.build();
		order.addItem(item(squidId, 50));
		order.addItem(item(kelpId, 20));
		orderCommandRepository.save(order);
	}

	private OrderItem item(UUID productId, int quantity) {
		return OrderItem.builder()
				.productId(productId)
				.quantity(quantity)
				.inventoryId(UUID.randomUUID())
				.build();
	}

	private OrderSnapshot capture(EventType eventType) {
		int nextSequence = orderSnapshotCommandRepository.findMaxSequenceByOrderId(order.getId()).orElse(0) + 1;
		return orderSnapshotCommandRepository.save(
				OrderSnapshot.capture(order, nextSequence, eventType, userId));
	}

	@Test
	@DisplayName("스냅샷은 주문의 모든 상품 줄을 복사해 담는다")
	void create() {
		// given & when
		OrderSnapshot snapshot = capture(EventType.ORDER_CREATED);

		// then
		assertThat(snapshot.getSequence()).isEqualTo(1);
		assertThat(snapshot.getOrderId()).isEqualTo(order.getId());
		assertThat(snapshot.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(snapshot.getItems()).hasSize(2)
				.extracting(OrderItemSnapshot::getProductId)
				.containsExactly(squidId, kelpId);
		assertThat(snapshot.getItems()).extracting(OrderItemSnapshot::getQuantity)
				.containsExactly(50, 20);
	}

	@Test
	@DisplayName("이력이 쌓이면 sequence 가 1부터 순서대로 붙고, 지난 이력은 그때 구성을 그대로 유지한다")
	void read() {
		// given
		capture(EventType.ORDER_CREATED);

		order.replaceItems(List.of(item(squidId, 10)));
		capture(EventType.ORDER_MODIFIED);

		order.cancel();
		capture(EventType.ORDER_CANCELED);

		// when
		Page<OrderSnapshot> timeline = orderSnapshotQueryRepository.findByOrderId(
				order.getId(), null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "sequence")));

		// then
		assertThat(timeline.getTotalElements()).isEqualTo(3);
		assertThat(timeline.getContent()).extracting(OrderSnapshot::getSequence).containsExactly(1, 2, 3);
		assertThat(timeline.getContent()).extracting(OrderSnapshot::getEventType)
				.containsExactly(EventType.ORDER_CREATED, EventType.ORDER_MODIFIED, EventType.ORDER_CANCELED);

		// 1번 이력은 수정 전 구성(2줄)을 그대로 들고 있다
		assertThat(timeline.getContent().get(0).getItems()).hasSize(2);
		assertThat(timeline.getContent().get(1).getItems()).hasSize(1);
		assertThat(timeline.getContent().get(2).getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
	}

	@Test
	@DisplayName("이벤트 종류로 이력을 걸러 볼 수 있다")
	void readByEventType() {
		// given
		capture(EventType.ORDER_CREATED);
		capture(EventType.ORDER_MODIFIED);
		capture(EventType.ORDER_MODIFIED);

		// when
		Page<OrderSnapshot> modified = orderSnapshotQueryRepository.findByOrderId(
				order.getId(), EventType.ORDER_MODIFIED, PageRequest.of(0, 10));

		// then
		assertThat(modified.getTotalElements()).isEqualTo(2);
		assertThat(modified.getContent()).extracting(OrderSnapshot::getSequence)
				.containsExactlyInAnyOrder(2, 3);
	}

	@Test
	@DisplayName("논리 삭제된 이력은 조회에서 빠진다")
	void delete() {
		// given
		OrderSnapshot first = capture(EventType.ORDER_CREATED);
		capture(EventType.ORDER_MODIFIED);

		// when
		first.softDelete(userId);

		// then
		assertThat(orderSnapshotQueryRepository.findDetailById(first.getId())).isEmpty();
		assertThat(orderSnapshotCommandRepository.findAllByOrderId(order.getId())).hasSize(1);
		assertThat(orderSnapshotQueryRepository
				.findByOrderId(order.getId(), null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
	}
}
