package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.OrderCreateRequest;

import java.util.List;
import java.util.UUID;

/**
 * 주문 접수 Use Case 에 필요한 입력 전부를 담는다.
 *
 * receiverUserId 는 인증 주체에서 오고 나머지는 요청 본문에서 오지만,
 * Service 는 그 출처를 알 필요가 없다 (팀문서 2026-08-05 컨벤션).
 */
public record OrderCreateCommand(
		UUID receiverUserId,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		String requestDetails,
		List<OrderItemCommand> items
) {

	public static OrderCreateCommand from(UUID receiverUserId, OrderCreateRequest request) {
		return new OrderCreateCommand(
				receiverUserId,
				request.supplierCompanyId(),
				request.receiverCompanyId(),
				request.requestDetails(),
				request.items().stream().map(OrderItemCommand::from).toList());
	}
}
