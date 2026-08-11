package com.delivery_project.order_service.order.application.command;

import com.delivery_project.order_service.order.presentation.request.InventoryInternalCreateRequest;

import java.util.UUID;

/**
 * 초기 재고 레코드 생성 Use Case 입력.
 *
 * <p>수량은 항상 0 으로 시작한다. 호출하는 쪽이 초기 수량을 정하게 두면 입고 이력 없이 재고가
 * 생겨서 수량이 어디서 왔는지 추적할 수 없다.
 *
 * <p>허브는 담지 않는다. 어느 허브에 만들지는 order 가 hub-service 에서 목록을 받아 정한다.
 */
public record InventoryInternalCreateCommand(
		UUID productId,
		UUID companyId
) {
	private static final int INITIAL_QUANTITY = 0;

	public static InventoryInternalCreateCommand from(InventoryInternalCreateRequest request) {
		return new InventoryInternalCreateCommand(request.productId(), request.companyId());
	}

	/** 등록 규칙은 외부 API 와 같아야 하므로 기존 Command 로 바꿔 넘긴다 */
	public InventoryCreateCommand toCreateCommand(UUID hubId) {
		return new InventoryCreateCommand(productId, hubId, companyId, INITIAL_QUANTITY);
	}
}
