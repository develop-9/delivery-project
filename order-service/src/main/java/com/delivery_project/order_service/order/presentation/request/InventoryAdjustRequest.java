package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/** 실사 재고 보정 — 누적이 아니라 덮어쓰기다 */
public record InventoryAdjustRequest(

		@NotNull(message = "보정 수량은 필수입니다.")
		@Min(value = 0, message = "보정 수량은 0 이상이어야 합니다.")
		Integer quantity,

		@NotBlank(message = "보정 사유는 필수입니다.")
		@Size(max = 255, message = "보정 사유는 255자를 넘을 수 없습니다.")
		String reason
) {
}
