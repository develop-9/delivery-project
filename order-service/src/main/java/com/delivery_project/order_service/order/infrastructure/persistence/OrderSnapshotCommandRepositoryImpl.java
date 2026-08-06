package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderSnapshotCommandRepositoryImpl implements OrderSnapshotCommandRepository {

	private final SpringDataOrderSnapshotRepository springDataOrderSnapshotRepository;

	@Override
	public OrderSnapshot save(OrderSnapshot snapshot) {
		return springDataOrderSnapshotRepository.save(snapshot);
	}

	@Override
	public Optional<Integer> findMaxSequenceByOrderId(UUID orderId) {
		return springDataOrderSnapshotRepository.findMaxSequenceByOrderId(orderId);
	}

	@Override
	public List<OrderSnapshot> findAllByOrderId(UUID orderId) {
		return springDataOrderSnapshotRepository.findByOrderIdAndDeletedAtIsNull(orderId);
	}
}
