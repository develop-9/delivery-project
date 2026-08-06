package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 주문 수정. 넘어온 필드만 반영한다(부분 수정).
 * items 가 null 이면 상품 구성은 그대로 두고, 비어 있지 않은 리스트가 오면 그 구성으로 맞춘다.
 */
public record OrderUpdateRequest(

		@Size(max = 500, message = "요청사항은 500자를 넘을 수 없습니다.")
		String requestDetails,

		@Size(min = 1, max = 20, message = "주문 상품은 1~20종이어야 합니다.")
		@Valid
		List<OrderItemRequest> items
) {
}
