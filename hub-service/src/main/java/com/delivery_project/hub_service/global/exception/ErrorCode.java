package com.delivery_project.hub_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 응답에 나가는 코드 문자열은 상수 이름({@link #name()})을 그대로 쓴다 ({@link ErrorDto}).
 * 별도 {@code code} 필드를 두면 상수 이름과 어긋날 수 있는데 둘이 달라야 할 이유가 없다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// common
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
	INVALID_STATE(HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
	EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다."),
	FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
	AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다."),
	AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력조건을 불충족하였습니다."),

	// hub
	PARENT_HUB_REQUIRED(HttpStatus.BAD_REQUEST, "일반 허브는 상위 허브가 필요합니다."),
	PARENT_HUB_NOT_MAIN(HttpStatus.BAD_REQUEST, "상위 허브는 중앙 허브여야 합니다."),
	PARENT_HUB_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "중앙 허브의 상위 허브는 서버가 자기 자신으로 설정합니다."),
	SAME_HUB_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "출발 허브와 도착 허브가 같을 수 없습니다."),
	INVALID_HUB_ROUTE_SEGMENT(HttpStatus.BAD_REQUEST, "Hub-and-Spoke 규칙에 어긋나는 구간입니다."),
	HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),
	PARENT_HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "상위 허브를 찾을 수 없습니다."),
	HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "허브 간 이동정보를 찾을 수 없습니다."),
	HUB_ROUTE_PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "경로 구간의 이동정보가 등록되어 있지 않습니다."),
	DUPLICATE_HUB_NAME(HttpStatus.CONFLICT, "이미 존재하는 허브명입니다."),
	DUPLICATE_HUB_ROUTE(HttpStatus.CONFLICT, "이미 존재하는 허브 간 이동정보입니다."),
	HUB_HAS_CHILDREN(HttpStatus.CONFLICT, "하위 허브가 있어 처리할 수 없습니다."),
	MAP_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "지도 API 호출에 실패했습니다."),
	MAP_ROUTE_NOT_FOUND(HttpStatus.BAD_GATEWAY, "지도 API에서 두 허브 간 경로를 찾지 못했습니다."),

	;

	private final HttpStatus httpStatus;
	private final String message;
}
