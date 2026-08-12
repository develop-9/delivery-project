package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * user-service {@code GET /internal/v1/users/{userId}} 응답.
 * {@code username} 은 로그인 ID 라 order 가 쓸 일이 없어 받지 않는다.
 */
public record UserInfoResponse(
		UUID userId,
		String name,
		String role,
		UUID hubId,
		UUID companyId
) {
}
