package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.config.SystemUser;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

	private final OrderSnapshotRepository orderSnapshotRepository;

	public OrderSnapshot capture(Order order, EventType eventType, String note) {
		int nextSequence = orderSnapshotRepository.findMaxSequenceByOrderId(order.getId()).orElse(0) + 1;

		OrderSnapshot snapshot = OrderSnapshot.capture(order, nextSequence, eventType, note, currentUserId());
		orderSnapshotRepository.save(snapshot);

		log.info("[스냅샷] 기록 : [{}] orderId={} sequence={} eventType={} status={}",
				snapshot.getId(), order.getId(), nextSequence, eventType, order.getStatus());

		return snapshot;
	}

	/**
	 * 이력 작성자.
	 * TODO JWT 파싱 필터가 들어오면 SystemUser 대체를 제거한다.
	 */
	private UUID currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return SystemUser.ID;
		}

		try {
			return UUID.fromString(authentication.getName());
		} catch (IllegalArgumentException e) {
			return SystemUser.ID;
		}
	}
}
