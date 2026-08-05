package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

	/** 줄까지 한 번에 가져온다 (N+1 방지) */
	@EntityGraph(attributePaths = "items")
	Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByIdAndDeletedAtIsNull(UUID id);
}
