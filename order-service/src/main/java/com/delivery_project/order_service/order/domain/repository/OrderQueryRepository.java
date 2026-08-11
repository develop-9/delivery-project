package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 주문 쿼리 측 포트. OrderQueryService 가 쓴다. */
public interface OrderQueryRepository {

	Optional<Order> findDetailById(UUID orderId);

	Page<Order> search(OrderSearchCondition condition, Pageable pageable);

	/**
	 * 업체가 공급자 또는 수령자로 들어간 주문 ID 전체.
	 *
	 * <p>delivery-service 가 COMPANY_MANAGER 의 배송 목록을 걸러낼 때 쓴다. 배송에는 업체 정보가
	 * 없고 {@code orderId} 만 있어서, 배송 건마다 주문을 조회하면 목록 크기만큼 호출이 나간다.
	 *
	 * <p>{@link OrderSearchCondition} 을 쓰지 않는 이유는 그 조건들이 AND 로 묶이기 때문이다.
	 * 여기서 필요한 것은 공급·수령 중 <b>하나라도</b> 맞는 주문이라 별도 쿼리로 둔다.
	 */
	List<UUID> findRelatedOrderIds(UUID companyId);
}
