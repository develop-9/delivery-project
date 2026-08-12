package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * company-service {@code GET /internal/v1/companies/{companyId}} 응답.
 *
 * <p>⚠️ {@code hubId}·{@code address} 는 아직 상대 응답에 없다. 두 값 모두
 * {@code p_companies} 에 {@code nullable = false} 로 존재하지만 내부 응답에서 빠져 있어
 * 추가를 요청해 둔 상태다. 반영 전까지는 {@code null} 로 내려온다.
 */
public record CompanyInfoResponse(
		UUID companyId,
		String name,
		String type,
		UUID hubId,
		String address
) {
}
