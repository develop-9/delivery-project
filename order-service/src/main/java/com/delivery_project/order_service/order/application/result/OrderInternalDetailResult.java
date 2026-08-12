package com.delivery_project.order_service.order.application.result;

import com.delivery_project.order_service.order.domain.entity.Order;

import java.util.List;
import java.util.UUID;

/**
 * 내부 API 의 주문 표현. 타 서비스가 쓰는 필드만 담는다.
 *
 * <p>slack-service 의 AI 파트가 발송 시한({@code final_dispatch_deadline})을 계산하는 데
 * 필요한 주문 정보다. 시한 자체는 AI 가 산출하는 값이라 order 가 넘기지 않는다.
 *
 * <p>상태·감사 필드는 뺐다. 호출하는 쪽이 "무엇을 몇 개 주문했나"만 알면 되고,
 * 주문 진행 상태는 order 내부 관심사다.
 */
public record OrderInternalDetailResult(
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
	}

	public static OrderInternalDetailResult from(Order order) {
		List<Item> items = order.getItems().stream()
				.map(i -> new Item(i.getProductId(), i.getQuantity()))
				.toList();

		return new OrderInternalDetailResult(
				order.getId(),
				order.getSupplierCompanyId(),
				order.getReceiverCompanyId(),
				order.getRequestDetails(),
				items);
	}
}
