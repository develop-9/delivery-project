package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderSnapshotRepository extends JpaRepository<OrderSnapshot, UUID> {

	@Query("SELECT MAX(s.sequence) FROM OrderSnapshot s WHERE s.orderId = :orderId")
	Optional<Integer> findMaxSequenceByOrderId(UUID orderId);

	@EntityGraph(attributePaths = "items")
	Page<OrderSnapshot> findByOrderIdAndDeletedAtIsNull(UUID orderId, Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Page<OrderSnapshot> findByOrderIdAndEventTypeAndDeletedAtIsNull(UUID orderId, EventType eventType,
			Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Optional<OrderSnapshot> findByIdAndDeletedAtIsNull(UUID id);

	List<OrderSnapshot> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
