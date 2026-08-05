package com.delivery_project.order_service.order.presentation.request;

import com.delivery_project.order_service.order.application.command.OrderCancelCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderCancelRequest(

		@NotBlank(message = "취소 사유는 필수입니다.")
		@Size(max = 255, message = "취소 사유는 255자를 넘을 수 없습니다.")
		String cancelReason
) {
	public OrderCancelCommand toCommand(UUID orderId) {
		return new OrderCancelCommand(orderId, cancelReason);
	}
}
