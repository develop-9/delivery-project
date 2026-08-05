package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.global.config.JpaConfig;
import com.delivery_project.order_service.global.config.SecurityAuditorAware;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.entity.OrderStatus;
import com.delivery_project.order_service.order.infrastructure.persistence.OrderQueryRepositoryImpl;
import com.delivery_project.order_service.order.infrastructure.persistence.OrderRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 주문 CRUD — 포트(OrderRepository/OrderQueryRepository) 기준으로 검증한다. */
@DataJpaTest
@Import({JpaConfig.class, SecurityAuditorAware.class, OrderRepositoryImpl.class, OrderQueryRepositoryImpl.class})
class OrderCrudTest {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderQueryRepository orderQueryRepository;

	private final UUID squidId = UUID.randomUUID();
	private final UUID kelpId = UUID.randomUUID();
	private final UUID hubId = UUID.randomUUID();

	private Order newOrder() {
		Order order = Order.builder()
				.supplierCompanyId(UUID.randomUUID())
				.receiverCompanyId(UUID.randomUUID())
				.originHubId(hubId)
				.destHubId(UUID.randomUUID())
				.requesterUserId(UUID.randomUUID())
				.requestDetails("오전 중 배송 부탁드립니다")
				.build();
		order.addItem(item(squidId, "마른 오징어", 50, "1000.00"));
		order.addItem(item(kelpId, "건조 다시마", 20, "500.00"));
		return order;
	}

	private OrderItem item(UUID productId, String productName, int quantity, String unitPrice) {
		return OrderItem.builder()
				.productId(productId)
				.productName(productName)
				.quantity(quantity)
				.unitPrice(new BigDecimal(unitPrice))
				.build();
	}

	@Test
	@DisplayName("주문을 생성하면 집계 컬럼과 상품 줄이 함께 저장된다")
	void create() {
		// given
		Order order = newOrder();

		// when
		Order saved = orderRepository.save(order);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(saved.getItemCount()).isEqualTo(2);
		assertThat(saved.getTotalQuantity()).isEqualTo(70);
		assertThat(saved.getTotalPrice()).isEqualByComparingTo("60000.00");
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getCreatedBy()).isNotNull();
	}

	@Test
	@DisplayName("저장한 주문을 상품 줄까지 조회할 수 있다")
	void read() {
		// given
		UUID orderId = orderRepository.save(newOrder()).getId();

		// when
		Optional<Order> found = orderQueryRepository.findDetailById(orderId);

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getItems()).hasSize(2)
				.extracting(OrderItem::getProductName)
				.containsExactlyInAnyOrder("마른 오징어", "건조 다시마");
	}

	@Test
	@DisplayName("상품 구성을 바꾸면 집계 컬럼이 다시 계산된다")
	void update() {
		// given
		Order order = orderRepository.save(newOrder());
		UUID laverId = UUID.randomUUID();

		// when — 오징어 수량 축소, 다시마 제거, 김 추가
		order.updateDetails("수량을 변경했습니다", null);
		order.replaceItems(List.of(
				item(squidId, "마른 오징어", 10, "1000.00"),
				item(laverId, "구운 김", 5, "2000.00")));

		// then
		Order found = orderQueryRepository.findDetailById(order.getId()).orElseThrow();
		assertThat(found.getRequestDetails()).isEqualTo("수량을 변경했습니다");
		assertThat(found.getItemCount()).isEqualTo(2);
		assertThat(found.getTotalQuantity()).isEqualTo(15);
		assertThat(found.getTotalPrice()).isEqualByComparingTo("20000.00");
		assertThat(found.getItems()).extracting(OrderItem::getProductId)
				.containsExactlyInAnyOrder(squidId, laverId);
	}

	@Test
	@DisplayName("논리 삭제된 주문은 조회와 검색에서 모두 빠진다")
	void delete() {
		// given
		Order order = orderRepository.save(newOrder());
		UUID deletedBy = UUID.randomUUID();

		// when — 삭제 API 는 아직 없고, 도메인 규칙만 검증한다
		order.delete(deletedBy);

		// then — 행은 남아 있지만 포트로는 조회되지 않는다
		assertThat(order.isDeleted()).isTrue();
		assertThat(order.getDeletedBy()).isEqualTo(deletedBy);
		assertThat(orderRepository.findById(order.getId())).isEmpty();
		assertThat(orderRepository.existsById(order.getId())).isFalse();
		assertThat(orderQueryRepository.findDetailById(order.getId())).isEmpty();
	}

	@Test
	@DisplayName("검색 조건으로 주문을 걸러낸다")
	void search() {
		// given
		orderRepository.save(newOrder());

		// when
		Page<Order> byStatus = orderQueryRepository.search(condition(OrderStatus.PENDING, null, null),
				PageRequest.of(0, 10));
		Page<Order> byKeyword = orderQueryRepository.search(condition(null, null, "오징어"),
				PageRequest.of(0, 10));
		Page<Order> byProduct = orderQueryRepository.search(condition(null, squidId, null),
				PageRequest.of(0, 10));
		Page<Order> noMatch = orderQueryRepository.search(condition(OrderStatus.COMPLETED, null, null),
				PageRequest.of(0, 10));

		// then
		assertThat(byStatus.getTotalElements()).isEqualTo(1);
		assertThat(byKeyword.getTotalElements()).isEqualTo(1);
		// 상품 2줄짜리 주문이라도 EXISTS 서브쿼리라 중복 집계되지 않는다
		assertThat(byProduct.getTotalElements()).isEqualTo(1);
		assertThat(noMatch.getTotalElements()).isZero();
	}

	private OrderSearchCondition condition(OrderStatus status, UUID productId, String keyword) {
		return new OrderSearchCondition(status, null, null, null, null, null, null,
				productId, keyword, null, null, null, null);
	}
}
