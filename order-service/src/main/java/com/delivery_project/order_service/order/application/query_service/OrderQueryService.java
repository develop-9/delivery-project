package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.OrderResult;
import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;
import com.delivery_project.order_service.order.application.result.OrderSummaryResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderSnapshot;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import com.delivery_project.order_service.order.domain.repository.OrderSnapshotQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

	private final OrderQueryRepository orderQueryRepository;
	private final OrderSnapshotQueryRepository orderSnapshotQueryRepository;

	/** 상세 조회 — 상품 줄은 @EntityGraph 로 한 번에 가져온다 */
	public OrderResult getOrder(UUID orderId) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		log.info("[주문] 단건 조회 : [{}] status={} itemCount={}",
				orderId, order.getStatus(), order.getItemCount());

		return OrderResult.from(order);
	}

	/**
	 * 검색 + 페이징.
	 * 정렬 컬럼과 페이지 크기는 PageableUtil 화이트리스트로 제한한다.
	 */
	public Page<OrderSummaryResult> searchOrders(OrderSearchCondition condition, Pageable pageable) {
		Pageable normalized = PageableUtil.normalize(pageable, PageableUtil.ORDER_SORTS);
		Page<Order> orders = orderQueryRepository.search(condition, normalized);

		log.info("[주문] 검색 : [status={}, originHubId={}, keyword={}] page={} size={} totalElements={}",
				condition.status(), condition.originHubId(), condition.keyword(),
				normalized.getPageNumber(), normalized.getPageSize(), orders.getTotalElements());

		return orders.map(OrderSummaryResult::from);
	}

	/**
	 * 주문 이력 타임라인.
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

	/** 이력 단건 조회 — 다른 주문의 이력을 경로만 바꿔 들여다보지 못하게 소속을 확인한다 */
	public OrderSnapshotResult getSnapshot(UUID orderId, UUID snapshotId) {
		OrderSnapshot snapshot = orderSnapshotQueryRepository.findDetailById(snapshotId)
				.filter(found -> found.getOrderId().equals(orderId))
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_SNAPSHOT_NOT_FOUND));

		log.info("[스냅샷] 단건 조회 : [{}] orderId={} sequence={} eventType={}",
				snapshotId, orderId, snapshot.getSequence(), snapshot.getEventType());

		return OrderSnapshotResult.from(snapshot);
	}
}
