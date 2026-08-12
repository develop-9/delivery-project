package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/** 목록·검색 한 줄. 상품 줄은 담지 않는다(페이지 크기만큼 추가 쿼리가 나간다). */
public record OrderSummaryResult(
		UUID orderId,
		String status,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID receiverUserId,
		Instant createdAt
) {
	public static OrderSummaryResult from(Order order) {
		return new OrderSummaryResult(
				order.getId(), order.getStatus().name(),
				order.getSupplierCompanyId(), order.getReceiverCompanyId(),
				order.getReceiverUserId(), order.getCreatedAt());
	}
}
