package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문 이력 조회.
 *
 * 주문과 이력은 관리하는 데이터와 책임이 달라 조회 서비스도 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderSnapshotQueryService {

	private final OrderSnapshotQueryRepository orderSnapshotQueryRepository;
	private final OrderQueryRepository orderQueryRepository;

	/**
	 * 이력 타임라인.
	 * eventType 이 주어지면 그 사건만 걸러 본다 (예: 수량이 몇 번 바뀌었나).
	 */
	public Page<OrderSnapshotResult> getSnapshots(UUID orderId, EventType eventType, Pageable pageable) {
		if (orderQueryRepository.findDetailById(orderId).isEmpty()) {
			throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
		}

		Page<OrderSnapshot> snapshots = orderSnapshotQueryRepository.findByOrderId(
				orderId, eventType, PageableUtil.normalize(pageable, PageableUtil.SNAPSHOT_SORTS));

		log.info("[스냅샷] 이력 조회 : [{}] eventType={} totalElements={}",
				orderId, eventType, snapshots.getTotalElements());

		return snapshots.map(OrderSnapshotResult::from);
	}

	/**
	 * 이력 단건 조회.
	 *
	 * <p>이력은 {@code /api/v1/order-snapshots/{orderSnapshotId}} 로 <b>독립 리소스</b>로 노출된다.
	 * 경로에 주문 ID 가 없으므로 "이 이력을 볼 자격이 있는가"를 <b>인증 주체로</b> 판단한다.
	 * 이력 → 주문 → {@code receiver_user_id} 를 따라가 호출자와 비교한다.
	 */
	public OrderSnapshotResult getSnapshot(UUID orderSnapshotId, UUID callerId) {
		OrderSnapshot snapshot = orderSnapshotQueryRepository.findDetailById(orderSnapshotId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND));

		validateAccessible(snapshot, callerId);

		log.info("[스냅샷] 단건 조회 : [{}] orderId={} sequence={} eventType={}",
				orderSnapshotId, snapshot.getOrderId(), snapshot.getSequence(), snapshot.getEventType());

		return OrderSnapshotResult.from(snapshot);
	}

	/**
	 * 남의 주문 이력을 ID 만 알아내 들여다보지 못하게 막는다.
	 *
	 * <p>없는 이력과 권한 없는 이력을 <b>같은 404 로</b> 응답한다. 403 을 내면 호출자가
	 * 상태코드 차이로 "그 ID 의 이력이 존재한다"는 사실을 알아낼 수 있다.
	 *
	 * <p>TODO JWT 파싱 필터가 붙기 전까지는 {@code callerId} 가 항상 null 이라 이 검증이 비어 있다.
	 * 필터가 붙으면 아래 null 스킵 분기를 제거한다.
	 * TODO MASTER·HUB_MANAGER 는 남의 주문 이력도 봐야 할 수 있다. role 이 principal 에
	 * 실리면(JwtPrincipal) 역할 기반 예외를 여기에 추가한다.
	 */
	private void validateAccessible(OrderSnapshot snapshot, UUID callerId) {
		if (callerId == null) {
			return;
		}

		Order order = orderQueryRepository.findDetailById(snapshot.getOrderId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND));

		if (!order.getReceiverUserId().equals(callerId)) {
			log.warn("[스냅샷] 권한 없는 이력 조회 차단 : [{}] orderId={} callerId={}",
					snapshot.getId(), snapshot.getOrderId(), callerId);
			throw new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND);
		}
	}
}
