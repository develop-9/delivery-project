package com.delivery_project.order_service.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ---------- 공통 ----------
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", "NOT_FOUND"),
    INVALID_STATE(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다.", "INVALID_STATE"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다.", "INVALID_INPUT_VALUE"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력조건을 불충족하였습니다.", "INVALID_REQUEST"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다.", "UNSUPPORTED_MEDIA_TYPE"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다.", "AUTH_UNAUTHORIZED"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "AUTH_FORBIDDEN"),
    INTERNAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 내부 API에 접근할 수 없습니다.", "INTERNAL_ACCESS_DENIED"),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "연동 서비스를 사용할 수 없습니다.", "EXTERNAL_SERVICE_UNAVAILABLE"),
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "잠시 후 다시 시도해 주세요.", "CONCURRENT_UPDATE_CONFLICT"),

    // ---------- Order ----------
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.", "ORDER_NOT_FOUND"),
    ORDER_SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "스냅샷을 찾을 수 없습니다.", "ORDER_SNAPSHOT_NOT_FOUND"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "현재 상태에서 처리할 수 없는 요청입니다.", "INVALID_ORDER_STATUS"),
    DELIVERY_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 배송이 시작되었습니다.", "DELIVERY_ALREADY_STARTED"),
    DUPLICATE_ORDER_ITEM(HttpStatus.BAD_REQUEST, "같은 상품을 여러 줄로 담을 수 없습니다.", "DUPLICATE_ORDER_ITEM"),
    ORDER_ITEM_REQUIRED(HttpStatus.BAD_REQUEST, "주문 상품은 1개 이상이어야 합니다.", "ORDER_ITEM_REQUIRED"),
    ORDER_ITEM_NOT_FOUND(HttpStatus.BAD_REQUEST, "주문에 없는 상품입니다.", "ORDER_ITEM_NOT_FOUND"),

    // ---------- Inventory (재고 연동 단계에서 사용) ----------
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다.", "INVENTORY_NOT_FOUND"),
    INVENTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 재고입니다.", "INVENTORY_ALREADY_EXISTS"),
    INVENTORY_IN_USE(HttpStatus.BAD_REQUEST, "사용 중인 재고는 삭제할 수 없습니다.", "INVENTORY_IN_USE"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다.", "INSUFFICIENT_STOCK"),

    // ────────── 서비스 연동 ──────────
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "업체를 찾을 수 없습니다.", "COMPANY_NOT_FOUND"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"),
    RECEIVER_COMPANY_MISMATCH(HttpStatus.FORBIDDEN, "본인이 속한 업체로만 주문할 수 있습니다.", "RECEIVER_COMPANY_MISMATCH"),
    DELIVERY_CREATE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "배송 생성에 실패했습니다.", "DELIVERY_CREATE_FAILED"),
    DELIVERY_CANCEL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "배송 취소에 실패했습니다.", "DELIVERY_CANCEL_FAILED"),
    ;

    private final HttpStatus status;
    private final String message;
    private final String code;

    ErrorCode(
            HttpStatus status,
            String message,
            String code
    ) {
        this.status = status;
        this.message = message;
        this.code = code;
    }
}
