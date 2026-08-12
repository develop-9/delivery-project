package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 내부 API 주문 상세 응답. Slack/AI/Delivery Service가 소비한다. */
public record OrderInternalDetailResponse(
		UUID orderId,
		String status,
		UUID productId,
		String productName,
		Integer quantity,
		UUID supplierCompanyId,
		String supplierCompanyName,
		UUID receiverCompanyId,
		String receiverCompanyName,
		UUID originHubId,
		UUID destHubId,
		UUID requesterUserId,
		String requesterName,
		String requestDetails,
		Instant createdAt,
		List<Item> items
) {
	public record Item(
			UUID productId,
			Integer quantity
	) {
		public static Item from(OrderInternalDetailResult.Item item) {
			return new Item(item.productId(), item.quantity());
		}
	}

	public static OrderInternalDetailResponse from(OrderInternalDetailResult result) {
		return new OrderInternalDetailResponse(
				result.orderId(),
				result.status(),
				result.productId(),
				result.productName(),
				result.quantity(),
				result.supplierCompanyId(),
				result.supplierCompanyName(),
				result.receiverCompanyId(),
				result.receiverCompanyName(),
				result.originHubId(),
				result.destHubId(),
				result.requesterUserId(),
				result.requesterName(),
				result.requestDetails(),
				result.createdAt(),
				result.items().stream().map(Item::from).toList());
	}
}
