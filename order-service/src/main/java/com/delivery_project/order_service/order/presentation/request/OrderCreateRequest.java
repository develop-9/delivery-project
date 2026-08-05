package com.delivery_project.order_service.order.presentation.request;

import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command.OrderItemCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 접수 요청.
 *
 * 1단계(독립 동작)에서는 공급업체·허브·상품 정보를 요청값으로 받는다.
 * company-service / hub-service 연동 단계에서
 * supplierCompanyId · originHubId · destHubId · productName · unitPrice 는
 * 서버가 조회한 값으로 대체되고 요청 필드에서 빠진다.
 */
public record OrderCreateRequest(

		@NotNull(message = "공급 업체 ID는 필수입니다.")
		UUID supplierCompanyId,

		@NotNull(message = "수령 업체 ID는 필수입니다.")
		UUID receiverCompanyId,

		@NotNull(message = "출발 허브 ID는 필수입니다.")
		UUID originHubId,

		@NotNull(message = "도착 허브 ID는 필수입니다.")
		UUID destHubId,

		@NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
		// min 을 생략하면 OpenAPI 에 minItems: 0 으로 실려 Swagger 예시에 빈 배열이 뜬다
		@Size(min = 1, max = 20, message = "한 주문에 상품은 1~20종이어야 합니다.")
		@Valid
		List<OrderItemRequest> items,

		@Size(max = 500, message = "요청사항은 500자를 넘을 수 없습니다.")
		String requestDetails,

		@Future(message = "납품 기한은 현재 시각 이후여야 합니다.")
		LocalDateTime dueAt
) {
	public record OrderItemRequest(

			@NotNull(message = "상품 ID는 필수입니다.")
			UUID productId,

			@NotBlank(message = "상품명은 필수입니다.")
			@Size(max = 100, message = "상품명은 100자를 넘을 수 없습니다.")
			String productName,

			@NotNull(message = "수량은 필수입니다.")
			@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
			Integer quantity,

			@NotNull(message = "단가는 필수입니다.")
			@DecimalMin(value = "0.0", message = "단가는 0 이상이어야 합니다.")
			@Digits(integer = 10, fraction = 2, message = "단가 형식이 올바르지 않습니다.")
			BigDecimal unitPrice
	) {
		public OrderItemCommand toCommand() {
			return new OrderItemCommand(productId, productName, quantity, unitPrice);
		}
	}

	/** 인증 주체(requesterUserId)까지 합쳐 Use Case 입력 하나로 만든다 */
	public OrderCreateCommand toCommand(UUID requesterUserId) {
		return new OrderCreateCommand(
				requesterUserId,
				supplierCompanyId, receiverCompanyId, originHubId, destHubId,
				requestDetails, dueAt,
				items.stream().map(OrderItemRequest::toCommand).toList());
	}
}
