package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderSummaryResult;

import java.time.Instant;
import java.util.UUID;

/** 목록·검색 응답 한 줄. 상품 줄은 담지 않는다 */
public record OrderSummaryResponse(
		UUID orderId,
		String status,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID receiverUserId,
		Instant createdAt
) {
	public static OrderSummaryResponse from(OrderSummaryResult result) {
		return new OrderSummaryResponse(
				result.orderId(), result.status(),
				result.supplierCompanyId(), result.receiverCompanyId(),
				result.receiverUserId(), result.createdAt());
	}
}
