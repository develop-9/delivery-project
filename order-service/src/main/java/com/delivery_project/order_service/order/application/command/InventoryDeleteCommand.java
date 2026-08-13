package com.delivery_project.order_service.order.application.command;

import java.util.UUID;

/**
 * 재고 논리 삭제 Use Case 입력.
 *
 * 요청 본문이 없는 Use Case 라 Request DTO 없이 경로변수와 인증 주체만 묶는다.
 * Service 는 두 값의 출처를 알 필요가 없다.
 */
public record InventoryDeleteCommand(
		UUID inventoryId,
		UUID deletedBy
) {

	public static InventoryDeleteCommand from(UUID inventoryId, UUID deletedBy) {
		return new InventoryDeleteCommand(inventoryId, deletedBy);
	}
}
