package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.authorization.OrderAccessPolicy;
import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

	private final OrderQueryRepository orderQueryRepository;
	private final OrderAccessPolicy orderAccessPolicy;

	/** 상세 조회 — 상품 줄은 @EntityGraph 로 한 번에 가져온다 */
	public OrderResult getOrder(UUID orderId, JwtPrincipal principal) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		orderAccessPolicy.validateReadable(order, principal);

		log.info("[주문] 단건 조회 : [{}] status={} itemCount={}",
				orderId, order.getStatus(), order.getItems().size());

		return OrderResult.from(order);
	}

	/**
	 * 주문 상세 조회 (내부 API — slack-service 의 AI 파트가 발송 시한 산출에 쓴다).
	 *
	 * 외부 조회와 같은 행을 읽지만 반환 필드가 다르다. 타 서비스에는 "무엇을 몇 개 주문했나"만
	 * 주고 상태·감사 필드는 내보내지 않는다.
	 */
	public OrderInternalDetailResult getOrderForInternal(UUID orderId) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		log.info("[주문] 상세 조회(내부) : [{}] itemCount={}", orderId, order.getItems().size());

		return OrderInternalDetailResult.from(order);
	}

	/**
	 * 검색 + 페이징.
	 * 정렬 컬럼과 페이지 크기는 PageableUtil 화이트리스트로 제한한다.
	 */
	/**
	 * 업체와 엮인 주문 ID 전체 (내부 API — delivery 의 COMPANY_MANAGER 배송 목록 필터용).
	 *
	 * <p>인가를 걸지 않는다. 내부 호출에는 사용자 토큰이 없고, 호출 측이 이미 자기 업체의
	 * {@code companyId} 로만 물어보기 때문이다.
	 */
	public List<UUID> getRelatedOrderIds(UUID companyId) {
		List<UUID> orderIds = orderQueryRepository.findRelatedOrderIds(companyId);

		log.info("[주문] 업체 관련 주문 ID 조회(내부) : companyId={} count={}", companyId, orderIds.size());

		return orderIds;
	}

	public Page<OrderSummaryResult> searchOrders(OrderSearchCondition condition, Pageable pageable,
			JwtPrincipal principal) {
		// 남의 주문이 목록에 섞이지 않도록 조건 단계에서 좁힌다
		OrderSearchCondition scoped = orderAccessPolicy.canSeeAllOrders(principal)
				? condition
				: condition.restrictedTo(principal.userId());

		Pageable normalized = PageableUtil.normalize(pageable, PageableUtil.ORDER_SORTS);
		Page<Order> orders = orderQueryRepository.search(scoped, normalized);

		log.info("[주문] 검색 : [status={}, receiverCompanyId={}, keyword={}] page={} size={} totalElements={}",
				scoped.status(), scoped.receiverCompanyId(), scoped.keyword(),
				normalized.getPageNumber(), normalized.getPageSize(), orders.getTotalElements());

		return orders.map(OrderSummaryResult::from);
	}
}
