package com.delivery_project.order_service.order.application.port;

import java.util.UUID;

/**
 * 상품 정보 조회 포트. order 는 상품명을 소유하지 않는다(company-service 의
 * p_products가 원본) — 내부 주문 요약 API가 상품명을 노출할 때 쓴다.
 */
public interface ProductPort {

	ProductInfo getProductInfo(UUID productId);

	record ProductInfo(UUID productId, String name) {
	}
}
