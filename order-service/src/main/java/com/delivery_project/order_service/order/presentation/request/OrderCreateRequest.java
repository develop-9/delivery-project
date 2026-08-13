package com.delivery_project.order_service.order.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 주문 접수 요청. 필드는 팀문서 p_orders 가 실제로 갖는 값만 받는다.
 *
 * 출발·도착 허브는 받지 않는다. 상품(p_products.hub_id)과 수령 업체(p_companies.hub_id)에서
 * 도출되는 값이라 클라이언트가 보낼 필요도, order 가 저장할 이유도 없다.
 * 수신자(receiverUserId)는 요청 본문이 아니라 인증 주체에서 온다.
 */
public record OrderCreateRequest(

		@NotNull(message = "공급 업체 ID는 필수입니다.")
		UUID supplierCompanyId,

		@NotNull(message = "수령 업체 ID는 필수입니다.")
		UUID receiverCompanyId,

		@NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
		// min 을 생략하면 OpenAPI 에 minItems: 0 으로 실려 Swagger 예시에 빈 배열이 뜬다
		@Size(min = 1, max = 20, message = "한 주문에 상품은 1~20종이어야 합니다.")
		@Valid
		List<OrderItemRequest> items,

		@Size(max = 500, message = "요청사항은 500자를 넘을 수 없습니다.")
		String requestDetails
) {
}
