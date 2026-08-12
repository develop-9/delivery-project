package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 내부 API 의 주문 표현. Slack/AI/Delivery Service가 쓴다.
 *
 * orderId/supplierCompanyId/receiverCompanyId/requestDetails/items는 항상 채워지고,
 * 그 아래 필드들은 include 파라미터로 확장 조회한 값이라 요청 안 하면 비어 있을 수 있다.
 * items는 기존 호출자(Delivery Service)와의 하위 호환을 위해 그대로 둔다 — 상품이 여러
 * 개인 주문도 있어서 productId/productName/quantity(첫 상품 기준 단일 값)만으로는
 * 전체 상품을 표현 못 한다.
 */
public record OrderInternalDetailResult(
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
	}

	public static OrderInternalDetailResult from(Order order) {
		List<Item> items = order.getItems().stream()
				.map(i -> new Item(i.getProductId(), i.getQuantity()))
				.toList();

		Item firstItem = items.isEmpty() ? null : items.getFirst();

		return new OrderInternalDetailResult(
				order.getId(),
				order.getStatus().name(),
				firstItem == null ? null : firstItem.productId(),
				null,
				firstItem == null ? null : firstItem.quantity(),
				order.getSupplierCompanyId(),
				null,
				order.getReceiverCompanyId(),
				null,
				null,
				null,
				order.getReceiverUserId(),
				null,
				order.getRequestDetails(),
				order.getCreatedAt(),
				items);
	}

	public OrderInternalDetailResult withEnrichment(
			String productName,
			String supplierCompanyName,
			String receiverCompanyName,
			UUID originHubId,
			UUID destHubId,
			String requesterName
	) {
		return new OrderInternalDetailResult(
				orderId,
				status,
				productId,
				productName,
				quantity,
				supplierCompanyId,
				supplierCompanyName,
				receiverCompanyId,
				receiverCompanyName,
				originHubId,
				destHubId,
				requesterUserId,
				requesterName,
				requestDetails,
				createdAt,
				items);
	}
}
