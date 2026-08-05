package com.delivery_project.order_service.order.domain.entity;

public enum OrderStatus {

    PENDING,     // 접수됨. 재고 선점 완료, 배송 생성 대기
    CONFIRMED,   // 배송·경로 생성 완료
    COMPLETED,   // 배송 완료. 이 시점에만 실물 재고 차감
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
            case CONFIRMED -> next == COMPLETED || next == CANCELED;
            default        -> false;   // COMPLETED / CANCELED / FAILED 는 종착역
        };
    }
}