package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.InventoryInternalCreateRequest;

import java.util.UUID;

/**
 * 초기 재고 레코드 생성 Use Case 입력.
 *
 * <p>수량은 항상 0, 허브는 order 가 정하고, 업체는 비워둔다 —
 * 자세한 근거는 {@link InventoryInternalCreateRequest} 에 있다.
 */
public record InventoryInternalCreateCommand(
		UUID productId
) {
	private static final int INITIAL_QUANTITY = 0;

	public static InventoryInternalCreateCommand from(InventoryInternalCreateRequest request) {
		return new InventoryInternalCreateCommand(request.productId());
	}

	/** 등록 규칙은 외부 API 와 같아야 하므로 기존 Command 로 바꿔 넘긴다 */
	public InventoryCreateCommand toCreateCommand(UUID hubId) {
		return new InventoryCreateCommand(productId, hubId, null, INITIAL_QUANTITY);
	}
}
