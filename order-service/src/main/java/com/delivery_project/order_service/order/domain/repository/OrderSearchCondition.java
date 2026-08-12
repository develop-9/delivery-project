package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.order.domain.entity.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 검색 조건. 모든 필드는 optional 이며, 채워진 것만 AND 로 묶인다.
 *
 * 조건은 p_orders 가 실제로 가진 컬럼만 건다. 허브·금액은 order 가 소유하지 않아
 * (허브는 company/hub, 금액은 product) 여기서 걸 수 없다.
 *
 * 쿼리 파라미터 예)
 * GET /api/v1/orders?status=PENDING&receiverCompanyId=...&keyword=오전&page=0&size=30&sort=createdAt,desc
 */
public record OrderSearchCondition(

        OrderStatus status,

        UUID supplierCompanyId,
        UUID receiverCompanyId,
        UUID receiverUserId,

        /** 이 상품이 포함된 주문만 */
        UUID productId,

        /** 요청사항 부분 일치 (대소문자 무시) */
        String keyword,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdTo
) {
    /**
     * 호출자 본인의 주문만 보도록 조건을 좁힌다.
     *
     * <p>조회한 뒤에 걸러내지 않고 <b>조건 자체에</b> 넣는다. 뒤에서 걸러내면
     * totalElements·totalPages 가 실제 권한 범위와 어긋나 페이지 수가 부풀려진다.
     */
    public OrderSearchCondition restrictedTo(UUID callerId) {
        return new OrderSearchCondition(status, supplierCompanyId, receiverCompanyId,
                callerId, productId, keyword, createdFrom, createdTo);
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public String keywordPattern() {
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
