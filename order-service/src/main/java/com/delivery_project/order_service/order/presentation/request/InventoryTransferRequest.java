package com.delivery_project.order_service.order.presentation.request;

import com.delivery_project.order_service.order.application.command.InventoryTransferCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** 허브 간 재고 이관 */
public record InventoryTransferRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId,

		@NotNull(message = "출발 허브 ID는 필수입니다.")
		UUID fromHubId,

		@NotNull(message = "도착 허브 ID는 필수입니다.")
		UUID toHubId,

		@NotNull(message = "이관 수량은 필수입니다.")
		@Min(value = 1, message = "이관 수량은 1 이상이어야 합니다.")
		Integer quantity,

		@Size(max = 255, message = "비고는 255자를 넘을 수 없습니다.")
		String note
) {
	public InventoryTransferCommand toCommand() {
		return new InventoryTransferCommand(productId, fromHubId, toHubId, quantity, note);
	}
}
