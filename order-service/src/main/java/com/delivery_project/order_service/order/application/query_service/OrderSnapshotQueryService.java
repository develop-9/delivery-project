package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.authorization.OrderAccessPolicy;
import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
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
	private final OrderAccessPolicy orderAccessPolicy;

	/**
	 * 이력 타임라인.
	 * eventType 이 주어지면 그 사건만 걸러 본다 (예: 수량이 몇 번 바뀌었나).
	 */
	public Page<OrderSnapshotResult> getSnapshots(UUID orderId, EventType eventType, Pageable pageable,
			JwtPrincipal principal) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		// 주문을 못 보는 사람은 그 주문의 이력도 못 본다
		orderAccessPolicy.validateReadable(order, principal);

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
	public OrderSnapshotResult getSnapshot(UUID orderSnapshotId, JwtPrincipal principal) {
		OrderSnapshot snapshot = orderSnapshotQueryRepository.findDetailById(orderSnapshotId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND));

		validateAccessible(snapshot, principal);

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
	 * <p>판단은 {@link OrderAccessPolicy} 에 맡긴다. 이력을 볼 수 있는 사람은 그 주문을 볼 수 있는
	 * 사람과 같아야 하고, 규칙이 두 군데로 갈라지면 주문은 막혔는데 이력은 뚫리는 일이 생긴다.
	 */
	private void validateAccessible(OrderSnapshot snapshot, JwtPrincipal principal) {
		Order order = orderQueryRepository.findDetailById(snapshot.getOrderId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND));

		try {
			orderAccessPolicy.validateReadable(order, principal);
		} catch (BusinessException exception) {
			// 정책은 주문 기준으로 ORDER_NOT_FOUND 를 던지는데, 여기서 찾던 것은 이력이다
			throw new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND);
		}
	}
}
