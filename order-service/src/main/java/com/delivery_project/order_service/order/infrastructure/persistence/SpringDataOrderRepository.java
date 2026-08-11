package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

	/** 줄까지 한 번에 가져온다 (N+1 방지) */
	@EntityGraph(attributePaths = "items")
	Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByIdAndDeletedAtIsNull(UUID id);

	/** ID 만 프로젝션한다. 엔티티를 다 읽으면 쓰지 않을 객체가 영속성 컨텍스트에 쌓인다 */
	@Query("SELECT o.id FROM Order o "
			+ "WHERE o.deletedAt IS NULL "
			+ "AND (o.supplierCompanyId = :companyId OR o.receiverCompanyId = :companyId)")
	List<UUID> findRelatedOrderIds(@Param("companyId") UUID companyId);
}
