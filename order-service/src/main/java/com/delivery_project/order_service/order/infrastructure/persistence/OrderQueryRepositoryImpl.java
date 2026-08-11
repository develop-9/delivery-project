package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

	private final SpringDataOrderRepository springDataOrderRepository;

	@Override
	public Optional<Order> findDetailById(UUID orderId) {
		return springDataOrderRepository.findByIdAndDeletedAtIsNull(orderId);
	}

	@Override
	public List<UUID> findRelatedOrderIds(UUID companyId) {
		return springDataOrderRepository.findRelatedOrderIds(companyId);
	}

	@Override
	public Page<Order> search(OrderSearchCondition condition, Pageable pageable) {
		return springDataOrderRepository.findAll(OrderSpecifications.from(condition), pageable);
	}
}
