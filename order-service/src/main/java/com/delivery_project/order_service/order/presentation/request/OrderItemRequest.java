package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 주문 상품 줄 하나.
 *
 * 상품명·단가는 받지 않는다. 원본이 company-service(p_products)에 있고,
 * 클라이언트가 보낸 값을 믿으면 가격을 조작할 수 있다.
 * 이력에 남길 "주문 당시 상품명·가격"은 연동 시 서버가 조회해 채운다.
 */
public record OrderItemRequest(

		@NotNull(message = "상품 ID는 필수입니다.")
		UUID productId,

		@NotNull(message = "수량은 필수입니다.")
		@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
		Integer quantity
) {
}
