package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderSnapshotQueryRepository {

	Page<OrderSnapshot> findByOrderId(UUID orderId, EventType eventType, Pageable pageable);

	Optional<OrderSnapshot> findDetailById(UUID snapshotId);
}
