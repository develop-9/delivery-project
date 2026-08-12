package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * 주문 커맨드 측 포트. 상태 변경과 불변식 검사를 위한 조회만 둔다.
 * 화면에 보여주기 위한 조회는 OrderQueryRepository 에 있다.
 */
public interface OrderCommandRepository {

	Order save(Order order);

	Optional<Order> findById(UUID orderId);

	boolean existsById(UUID orderId);
}
