package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderSnapshotQueryRepositoryImpl implements OrderSnapshotQueryRepository {

	private final SpringDataOrderSnapshotRepository springDataOrderSnapshotRepository;

	@Override
	public Page<OrderSnapshot> findByOrderId(UUID orderId, EventType eventType, Pageable pageable) {
		return eventType == null
				? springDataOrderSnapshotRepository.findByOrderIdAndDeletedAtIsNull(orderId, pageable)
				: springDataOrderSnapshotRepository.findByOrderIdAndEventTypeAndDeletedAtIsNull(
						orderId, eventType, pageable);
	}

	@Override
	public Optional<OrderSnapshot> findDetailById(UUID snapshotId) {
		return springDataOrderSnapshotRepository.findByIdAndDeletedAtIsNull(snapshotId);
	}
}
