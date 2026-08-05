package com.delivery_project.order_service.order.presentation.request;

import com.delivery_project.order_service.order.application.command.InventoryCreateCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 재고 등록 — 상품을 특정 허브에 배치한다. 수량 증가가 아니다(그건 입고).
 *
 * companyId 는 원래 company-service 에서 상품으로 도출해야 하는 값이다.
 * 연동 전까지는 요청값으로 받고, 연동 시 요청 필드에서 빠진다.
 */
public record InventoryCreateRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId,

		@NotNull(message = "허브 ID는 필수입니다.")
		UUID hubId,

		@NotNull(message = "업체 ID는 필수입니다.")
		UUID companyId,

		@NotNull(message = "초기 수량은 필수입니다.")
		@Min(value = 0, message = "초기 수량은 0 이상이어야 합니다.")
		Integer quantity
) {
	public InventoryCreateCommand toCommand() {
		return new InventoryCreateCommand(productId, hubId, companyId, quantity);
	}
}
