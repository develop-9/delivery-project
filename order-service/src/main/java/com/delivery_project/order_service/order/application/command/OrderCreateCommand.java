package com.delivery_project.order_service.order.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 접수 Use Case 에 필요한 입력 전부를 담는다.
 * requesterUserId 는 인증 주체에서 오고 나머지는 요청 본문에서 오지만,
 * Service 는 그 출처를 알 필요가 없다.
 */
public record OrderCreateCommand(
		UUID requesterUserId,
		UUID supplierCompanyId,
		UUID receiverCompanyId,
		UUID originHubId,
		UUID destHubId,
		String requestDetails,
		LocalDateTime dueAt,
		List<OrderItemCommand> items
) {
}
