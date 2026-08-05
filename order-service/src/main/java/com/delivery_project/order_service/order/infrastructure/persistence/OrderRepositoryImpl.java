package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

	private final SpringDataOrderRepository springDataOrderRepository;

	@Override
	public Order save(Order order) {
		return springDataOrderRepository.save(order);
	}

	@Override
	public Optional<Order> findById(UUID orderId) {
		return springDataOrderRepository.findByIdAndDeletedAtIsNull(orderId);
	}

	@Override
	public boolean existsById(UUID orderId) {
		return springDataOrderRepository.existsByIdAndDeletedAtIsNull(orderId);
	}
}
