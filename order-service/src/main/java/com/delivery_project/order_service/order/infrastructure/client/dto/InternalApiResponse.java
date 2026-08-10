package com.delivery_project.order_service.order.infrastructure.client.dto;

/**
 * 내부 API 공통 응답 봉투. 팀 공통 {@code SuccessResponse} 중 연동에 필요한 두 필드만 받는다.
 *
 * <p>{@code code}·{@code message}·{@code timestamp} 는 읽지 않으므로 선언하지 않는다.
 * (Jackson 은 모르는 필드를 무시하도록 기본 설정돼 있다.)
 */
public record InternalApiResponse<T>(
		boolean success,
		T data
) {
}
