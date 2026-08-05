package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;
import com.delivery_project.order_service.order.domain.entity.EventType;
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
