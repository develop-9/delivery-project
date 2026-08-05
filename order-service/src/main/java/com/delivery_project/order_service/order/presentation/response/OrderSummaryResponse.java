package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderSummaryResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** 목록·검색 응답 한 줄. 상품 줄은 담지 않는다 */
public record OrderSummaryResponse(
		UUID orderId,
		String status,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID originHubId,
		UUID destHubId,
		UUID requesterUserId,
		Integer itemCount,
		Integer totalQuantity,
		BigDecimal totalPrice,
		UUID deliveryId,
		LocalDateTime dueAt,
		Instant createdAt
) {
	public static OrderSummaryResponse from(OrderSummaryResult result) {
		return new OrderSummaryResponse(
				result.orderId(), result.status(),
				result.supplierCompanyId(), result.receiverCompanyId(),
				result.originHubId(), result.destHubId(), result.requesterUserId(),
				result.itemCount(), result.totalQuantity(), result.totalPrice(),
				result.deliveryId(), result.dueAt(), result.createdAt());
	}
}
