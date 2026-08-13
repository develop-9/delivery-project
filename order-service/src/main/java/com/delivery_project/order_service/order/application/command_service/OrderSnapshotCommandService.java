package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	private final OrderSnapshotCommandRepository orderSnapshotCommandRepository;

	/**
	 * 이력 작성자 공급자.
	 *
	 * 스냅샷의 {@code created_by} 는 엔티티가 직접 채우므로(@CreatedBy 미사용) JPA 감사와 같은
	 * 판단 기준을 여기서도 그대로 써야 한다. 같은 로직을 복사하지 않고 감사자 빈을 재사용한다.
	 */
	private final AuditorAware<UUID> auditorAware;

	public OrderSnapshot capture(Order order, EventType eventType) {
		int nextSequence = orderSnapshotCommandRepository.findMaxSequenceByOrderId(order.getId()).orElse(0) + 1;

		OrderSnapshot snapshot = OrderSnapshot.capture(order, nextSequence, eventType, currentUserId());
		orderSnapshotCommandRepository.save(snapshot);

		log.info("[스냅샷] 기록 : [{}] orderId={} sequence={} eventType={} status={}",
				snapshot.getId(), order.getId(), nextSequence, eventType, order.getStatus());

		return snapshot;
	}

	/** 같은 사건이 이미 남아 있는지. 외부 서비스가 같은 통보를 두 번 보내는 경우를 거른다 */
	@Transactional(readOnly = true)
	public boolean alreadyCaptured(UUID orderId, EventType eventType) {
		return orderSnapshotCommandRepository.existsByOrderIdAndEventType(orderId, eventType);
	}

	private UUID currentUserId() {
		return auditorAware.getCurrentAuditor()
				.orElseThrow(() -> new IllegalStateException("감사자를 확인할 수 없습니다."));
	}
}
