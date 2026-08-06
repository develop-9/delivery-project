package com.delivery_project.order_service.order.domain.entity;

/**
 * 주문 상태. 팀문서 p_orders.status 정의(PENDING / CONFIRMED / CANCELED / FAILED)를 따른다.
 *
 * <p>배송 완료는 주문 상태가 아니라 배송 상태(p_deliveries.status = COMPLETED)다.
 * 배송이 끝나면 주문 상태는 CONFIRMED 로 두고 이력에 ORDER_COMPLETED 만 남긴다.
 */
public enum OrderStatus {

    PENDING,     // 접수됨. 재고 선점 완료, 배송 생성 대기
    CONFIRMED,   // 배송·경로 생성 완료
    CANCELED,    // 취소됨. 선점 복원
    FAILED;      // 배송 생성 실패. 선점 복원 + 원인 추적용으로 남김

    public boolean isModifiable() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isCancelable() {
        return this == PENDING || this == CONFIRMED;
    }

    /** 상태 기계. 여기 없는 전이는 전부 막는다 */
    public boolean canTransitTo(OrderStatus next) {
        return switch (this) {
            case PENDING   -> next == CONFIRMED || next == CANCELED || next == FAILED;
            case CONFIRMED -> next == CANCELED || next == FAILED;
            default        -> false;   // CANCELED / FAILED 는 종착역
        };
    }
}
