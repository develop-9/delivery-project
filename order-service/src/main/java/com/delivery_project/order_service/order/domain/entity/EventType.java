package com.delivery_project.order_service.order.domain.entity;

public enum EventType {

    ORDER_CREATED,        // 주문 접수
    ORDER_MODIFIED,       // 수량·구성 변경
    DELIVERY_CONFIRMED,   // 배송 생성 완료
    ORDER_COMPLETED,      // 배송 완료
    ORDER_CANCELED,       // 취소
    ORDER_FAILED;         // 실패(보상 완료)

    public static EventType from(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> DELIVERY_CONFIRMED;
            case COMPLETED -> ORDER_COMPLETED;
            case CANCELED  -> ORDER_CANCELED;
            case FAILED    -> ORDER_FAILED;
            case PENDING   -> ORDER_CREATED;
        };
    }
}