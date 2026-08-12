package com.delivery_project.user_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 400
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	INVALID_BATCH_SIZE(HttpStatus.BAD_REQUEST, "한 번에 조회할 수 있는 최대 개수를 초과했습니다."),

	// 401
	AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
	AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
	AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

	// 403
	AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	USER_NOT_APPROVED(HttpStatus.FORBIDDEN, "승인되지 않은 계정입니다."),
	APPROVE_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "승인 권한이 없습니다."),
	REJECT_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "거절 권한이 없습니다."),
	READ_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "승인 대기자 조회 권한이 없습니다."),
	HUB_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "담당 허브로 신청한 사용자만 처리할 수 있습니다."),
	DELETE_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "사용자 삭제 권한이 없습니다."),
	SUSPEND_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "계정 정지 권한이 없습니다."),
	REINSTATE_USER_FORBIDDEN(HttpStatus.FORBIDDEN, "계정 정지 해제 권한이 없습니다."),

	// 404
	NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	INTERNAL_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "조건에 맞는 사용자를 찾을 수 없습니다."),
	HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 허브입니다."),
	COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업체입니다."),

	// 409
	INVALID_STATE(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다."),
	USER_DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	USER_DUPLICATE_SLACK_ID(HttpStatus.CONFLICT, "이미 등록된 Slack ID입니다."),
	USER_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 가입 신청입니다."),
	LAST_MASTER_DELETE_FORBIDDEN(HttpStatus.CONFLICT, "마지막 MASTER 계정은 삭제할 수 없습니다."),
	LAST_MASTER_SUSPEND_FORBIDDEN(HttpStatus.CONFLICT, "마지막 MASTER 계정은 정지할 수 없습니다."),
	USER_NOT_SUSPENDABLE(HttpStatus.CONFLICT, "승인된 사용자만 정지할 수 있습니다."),
	USER_NOT_SUSPENDED(HttpStatus.CONFLICT, "정지된 사용자만 정지 해제할 수 있습니다."),
	DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY(HttpStatus.CONFLICT, "담당 중인 배송이 있어 삭제할 수 없습니다."),

	// 415
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

	// 503
	DELIVERY_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "배송 담당자 정보를 확인할 수 없어 잠시 후 다시 시도해주세요."),
	HUB_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "허브 정보를 확인할 수 없어 잠시 후 다시 시도해주세요."),
	COMPANY_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "업체 정보를 확인할 수 없어 잠시 후 다시 시도해주세요."),

	// 500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
