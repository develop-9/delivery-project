package com.delivery_project.order_service.order.domain.entity;

/** 스냅샷을 발생시킨 사건. 팀문서 p_order_snapshots.event_type 정의를 따른다. */
public enum EventType {

    ORDER_CREATED,        // 주문 접수
    ORDER_MODIFIED,       // 구성·요청사항 변경
    DELIVERY_CONFIRMED,   // 배송 생성 완료
    ORDER_COMPLETED,      // 배송 완료 (주문 상태는 CONFIRMED 그대로)
    ORDER_CANCELED,       // 취소
    ORDER_FAILED;         // 실패(보상 완료)

    /**
     * 주문 상태 전이로 생기는 사건을 고른다.
     * ORDER_COMPLETED 는 대응하는 주문 상태가 없어(배송 상태다) 여기서 나오지 않는다.
     */
    public static EventType from(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> DELIVERY_CONFIRMED;
            case CANCELED  -> ORDER_CANCELED;
            case FAILED    -> ORDER_FAILED;
            case PENDING   -> ORDER_CREATED;
        };
    }
}
