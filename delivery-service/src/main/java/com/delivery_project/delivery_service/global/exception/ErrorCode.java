package com.delivery_project.delivery_service.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", "NOT_FOUND"),
    INVALID_STATE(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다.", "INVALID_STATE"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다.", "INVALID_INPUT_VALUE"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다.", "UNSUPPORTED_MEDIA_TYPE"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.", "EMPTY_FILE"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.", "FILE_UPLOAD_FAILED"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다.", "AUTH_UNAUTHORIZED"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "AUTH_FORBIDDEN"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력조건을 불충족하였습니다.", "INVALID_REQUEST"),

    // Delivery ErrorCode
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 배송이 생성된 주문입니다.", "DELIVERY_ALREADY_EXISTS"),
    DELIVERY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 생성에 실패했습니다.", "DELIVERY_CREATE_FAILED"),
    DELIVERY_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 배송입니다.", "DELIVERY_ALREADY_CANCELED"),
    DELIVERY_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 배송을 취소할 수 없습니다.", "DELIVERY_CANCEL_NOT_ALLOWED"),
    DELIVERY_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작된 배송은 수정할 수 없습니다." ,"DELIVERY_ALREADY_STARTED"),
    DELIVERY_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 배송을 삭제할 수 없습니다.", "DELIVERY_DELETE_NOT_ALLOWED"),
    UPDATE_DELIVERY_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 수정 권한이 없습니다.", "UPDATE_DELIVERY_FORBIDDEN"),
    DELETE_DELIVERY_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 삭제 권한이 없습니다.", "DELETE_DELIVERY_FORBIDDEN"),
    READ_DELIVERY_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 조회 권한이 없습니다.", "READ_DELIVERY_FORBIDDEN"),

    // DeliveryRoute ErrorCode
    DELIVERY_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 경로를 찾을 수 없습니다.", "DELIVERY_ROUTE_NOT_FOUND"),
    INVALID_ROUTE_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 배송 경로 상태 변경입니다.", "INVALID_ROUTE_STATUS_TRANSITION"),
    PREVIOUS_ROUTE_NOT_ARRIVED(HttpStatus.CONFLICT, "이전 배송 경로가 아직 완료되지 않았습니다.", "PREVIOUS_ROUTE_NOT_ARRIVED"),
    HUB_DELIVERY_MANAGER_NOT_AVAILABLE(HttpStatus.CONFLICT, "배정 가능한 허브 배송 담당자가 없습니다.", "HUB_DELIVERY_MANAGER_NOT_AVAILABLE"),
    COMPANY_DELIVERY_MANAGER_NOT_AVAILABLE(HttpStatus.CONFLICT, "배정 가능한 업체 배송 담당자가 없습니다.", "COMPANY_DELIVERY_MANAGER_NOT_AVAILABLE"),
    INVALID_DELIVERY_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 배송 상태 변경입니다.", "INVALID_DELIVERY_STATUS_TRANSITION"),
    COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED(HttpStatus.CONFLICT, "업체 배송 담당자가 배정되지 않았습니다.", "COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED"),
    DELIVERY_ROUTE_ALREADY_IN_TRANSIT(HttpStatus.CONFLICT, "이미 진행 중인 배송 경로가 존재합니다.", "DELIVERY_ROUTE_ALREADY_IN_TRANSIT"),
    DELIVERY_MANAGER_NOT_AVAILABLE(HttpStatus.CONFLICT, "배정 가능한 배송 담당자가 아닙니다.", "DELIVERY_MANAGER_NOT_AVAILABLE"),
    READ_DELIVERY_ROUTE_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 경로 조회 권한이 없습니다.", "READ_DELIVERY_ROUTE_FORBIDDEN"),

    //Delivery Manager ErrorCode
    DELIVERY_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 담당자를 찾을 수 없습니다.", "DELIVERY_MANAGER_NOT_FOUND"),
    DELIVERY_MANAGER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 배송 담당자입니다.", "DELIVERY_MANAGER_ALREADY_EXISTS"),
    INVALID_DELIVERY_MANAGER_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 배송 담당자 유형입니다.", "INVALID_DELIVERY_MANAGER_TYPE"),
    INVALID_HUB_DELIVERY_MANAGER(HttpStatus.BAD_REQUEST, "허브 배송 담당자는 소속 허브를 가질 수 없습니다.", "INVALID_HUB_DELIVERY_MANAGER"),
    INVALID_COMPANY_DELIVERY_MANAGER(HttpStatus.BAD_REQUEST, "업체 배송 담당자는 소속 허브가 필요합니다.", "INVALID_COMPANY_DELIVERY_MANAGER"),
    INVALID_PAGE_NUMBER(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.", "INVALID_PAGE_NUMBER"),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "페이지 크기는 10, 30, 50만 가능합니다.", "INVALID_PAGE_SIZE"),
    UPDATE_DELIVERY_MANAGER_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 담당자 수정 권한이 없습니다.", "UPDATE_DELIVERY_MANAGER_FORBIDDEN"),
    HUB_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "담당 허브의 배송 담당자만 처리할 수 있습니다.", "HUB_PERMISSION_DENIED"),
    DELIVERY_MANAGER_IS_DELIVERING(HttpStatus.CONFLICT, "배송 중인 담당자는 처리할 수 없습니다.", "DELIVERY_MANAGER_IS_DELIVERING"),
    DELETE_DELIVERY_MANAGER_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 담당자 삭제 권한이 없습니다.", "DELETE_DELIVERY_MANAGER_FORBIDDEN"),
    INTERNAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 내부 API에 접근할 수 없습니다.", "INTERNAL_ACCESS_DENIED"),
    ACTIVE_DELIVERY_EXISTS(HttpStatus.CONFLICT, "담당 중인 배송이 존재합니다.", "ACTIVE_DELIVERY_EXISTS"),
    DELIVERY_SEQUENCE_CONFLICT(HttpStatus.CONFLICT, "배송 담당자 배정 순번이 충돌했습니다.", "DELIVERY_SEQUENCE_CONFLICT"),
    DELIVERY_MANAGER_NOT_DELIVERING(HttpStatus.CONFLICT, "배송 중인 담당자가 아닙니다.", "DELIVERY_MANAGER_NOT_DELIVERING"),
    READ_DELIVERY_MANAGER_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 담당자 조회 권한이 없습니다.", "READ_DELIVERY_MANAGER_FORBIDDEN"),
    MANAGE_DELIVERY_MANAGER_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 담당자 관리 권한이 없습니다.", "MANAGE_DELIVERY_MANAGER_FORBIDDEN"),
    DELIVERY_MANAGER_NOT_ACTIVE(HttpStatus.CONFLICT, "비활성화된 배송 담당자는 배송에 배정할 수 없습니다.", "DELIVERY_MANAGER_NOT_ACTIVE"),
    // external service
    USER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "User Service를 사용할 수 없습니다.", "USER_SERVICE_UNAVAILABLE"),
    HUB_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Hub Service를 사용할 수 없습니다.", "HUB_SERVICE_UNAVAILABLE"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다.", "HUB_NOT_FOUND"),
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "허브 간 배송 경로를 찾을 수 없습니다.", "HUB_ROUTE_NOT_FOUND"),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.","ORDER_NOT_FOUND"),
    ORDER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Order Service를 사용할 수 없습니다.", "ORDER_SERVICE_UNAVAILABLE"),
    // 검증
    UPDATE_DELIVERY_ROUTE_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 경로 수정 권한이 없습니다.", "UPDATE_DELIVERY_ROUTE_FORBIDDEN"),
    UPDATE_DELIVERY_STATUS_FORBIDDEN(HttpStatus.FORBIDDEN, "배송 상태 변경 권한이 없습니다.", "UPDATE_DELIVERY_STATUS_FORBIDDEN"),
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