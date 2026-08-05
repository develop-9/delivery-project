package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.security.UserContext;
import com.delivery_project.order_service.global.security.UserContextHolder;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 주문 이력(스냅샷) 기록.
 *
 * 반드시 주문을 바꾼 트랜잭션 안에서 호출한다.
 * 주문 변경은 커밋됐는데 이력만 빠지면 왜 바뀌었는지를 영원히 알 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderSnapshotCommandService {

	/** 시스템(내부 호출·배치)이 남긴 이력의 작성자. JpaAuditingConfig 와 같은 값 */
	private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private final OrderSnapshotRepository orderSnapshotRepository;

	public OrderSnapshot capture(Order order, EventType eventType, String note) {
		int nextSequence = orderSnapshotRepository.findMaxSequenceByOrderId(order.getId()).orElse(0) + 1;

		OrderSnapshot snapshot = OrderSnapshot.capture(order, nextSequence, eventType, note, currentUserId());
		orderSnapshotRepository.save(snapshot);

		log.info("[스냅샷] 기록 : [{}] orderId={} sequence={} eventType={} status={}",
				snapshot.getId(), order.getId(), nextSequence, eventType, order.getStatus());

		return snapshot;
	}

	/** 주문이 논리 삭제되면 그 주문의 이력도 함께 감춘다 */
	public void softDeleteAllByOrder(UUID orderId) {
		List<OrderSnapshot> snapshots = orderSnapshotRepository.findAllByOrderId(orderId);
		UUID userId = currentUserId();

		snapshots.forEach(snapshot -> snapshot.softDelete(userId));

		log.info("[스냅샷] 삭제 : [{}] count={}", orderId, snapshots.size());
	}

	private UUID currentUserId() {
		UserContext context = UserContextHolder.get();
		return context != null ? context.userId() : SYSTEM_USER_ID;
	}
}
