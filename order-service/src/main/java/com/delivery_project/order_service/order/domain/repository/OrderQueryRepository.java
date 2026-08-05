package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** 주문 쿼리 측 포트. OrderQueryService 가 쓴다. */
public interface OrderQueryRepository {

	Optional<Order> findDetailById(UUID orderId);

	Page<Order> search(OrderSearchCondition condition, Pageable pageable);
}
