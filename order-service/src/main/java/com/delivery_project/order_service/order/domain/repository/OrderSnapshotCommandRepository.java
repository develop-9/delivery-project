package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderSnapshotCommandRepository {

	OrderSnapshot save(OrderSnapshot snapshot);

	Optional<Integer> findMaxSequenceByOrderId(UUID orderId);

	List<OrderSnapshot> findAllByOrderId(UUID orderId);

	/** 같은 사건이 이미 기록됐는지. 외부 서비스의 재시도를 걸러내는 데 쓴다 */
	boolean existsByOrderIdAndEventType(UUID orderId, EventType eventType);
}
