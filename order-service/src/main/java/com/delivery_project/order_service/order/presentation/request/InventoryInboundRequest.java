package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record InventoryInboundRequest(

		@NotNull(message = "입고 수량은 필수입니다.")
		@Min(value = 1, message = "입고 수량은 1 이상이어야 합니다.")
		Integer quantity,

		@Size(max = 255, message = "비고는 255자를 넘을 수 없습니다.")
		String note
) {
}
