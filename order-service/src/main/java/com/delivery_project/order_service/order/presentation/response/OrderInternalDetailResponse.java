package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;

import java.util.List;
import java.util.UUID;

/** 내부 API 주문 상세 응답. slack-service 의 AI 파트가 소비한다. */
public record OrderInternalDetailResponse(
		UUID orderId,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		String requestDetails,
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
				result.supplierCompanyId(),
				result.receiverCompanyId(),
				result.requestDetails(),
				result.items().stream().map(Item::from).toList());
	}
}
