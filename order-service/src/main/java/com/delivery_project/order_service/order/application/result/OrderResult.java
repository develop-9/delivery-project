package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 상세 결과. 생성·수정·취소·조회가 모두 같은 모양을 돌려주므로
 * 행동별 Result 를 따로 두지 않고 하나를 공유한다(동일 필드 4벌 중복 방지).
 */
public record OrderResult(
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
		List<Item> items,
		UUID deliveryId,
		String requestDetails,
		String cancelReason,
		LocalDateTime dueAt,
		LocalDateTime dispatchDeadlineAt,
		boolean dispatchOverdue,
		Instant createdAt,
		Instant updatedAt
) {
	public record Item(
			UUID orderItemId,
			UUID productId,
			String productName,
			Integer quantity,
			BigDecimal unitPrice,
			BigDecimal linePrice,
			UUID inventoryId
	) {
		public static Item from(OrderItem item) {
			return new Item(item.getId(), item.getProductId(), item.getProductName(),
					item.getQuantity(), item.getUnitPrice(), item.getLinePrice(), item.getInventoryId());
		}
	}

	public static OrderResult from(Order order) {
		return new OrderResult(
				order.getId(),
				order.getStatus().name(),
				order.getSupplierCompanyId(),
				order.getReceiverCompanyId(),
				order.getOriginHubId(),
				order.getDestHubId(),
				order.getRequesterUserId(),
				order.getItemCount(),
				order.getTotalQuantity(),
				order.getTotalPrice(),
				order.getItems().stream().map(Item::from).toList(),
				order.getDeliveryId(),
				order.getRequestDetails(),
				order.getCancelReason(),
				order.getDueAt(),
				order.getDispatchDeadlineAt(),
				order.isDispatchOverdue(),
				order.getCreatedAt(),
				order.getUpdatedAt());
	}
}
