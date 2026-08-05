package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderSnapshotRepository {

	OrderSnapshot save(OrderSnapshot snapshot);

	Optional<Integer> findMaxSequenceByOrderId(UUID orderId);

	List<OrderSnapshot> findAllByOrderId(UUID orderId);
}
