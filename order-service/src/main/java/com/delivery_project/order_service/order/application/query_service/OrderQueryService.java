package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.result.OrderResult;
import com.delivery_project.order_service.order.application.result.OrderSummaryResult;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
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

	/** 상세 조회 — 상품 줄은 @EntityGraph 로 한 번에 가져온다 */
	public OrderResult getOrder(UUID orderId) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		log.info("[주문] 단건 조회 : [{}] status={} itemCount={}",
				orderId, order.getStatus(), order.getItems().size());

		return OrderResult.from(order);
	}

	/**
	 * 검색 + 페이징.
	 * 정렬 컬럼과 페이지 크기는 PageableUtil 화이트리스트로 제한한다.
	 */
	public Page<OrderSummaryResult> searchOrders(OrderSearchCondition condition, Pageable pageable) {
		Pageable normalized = PageableUtil.normalize(pageable, PageableUtil.ORDER_SORTS);
		Page<Order> orders = orderQueryRepository.search(condition, normalized);

		log.info("[주문] 검색 : [status={}, receiverCompanyId={}, keyword={}] page={} size={} totalElements={}",
				condition.status(), condition.receiverCompanyId(), condition.keyword(),
				normalized.getPageNumber(), normalized.getPageSize(), orders.getTotalElements());

		return orders.map(OrderSummaryResult::from);
	}
}
