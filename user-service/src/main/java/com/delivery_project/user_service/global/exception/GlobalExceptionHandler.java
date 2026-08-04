package com.delivery_project.user_service.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.delivery_project.user_service.global.response.CommonResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException e) {
		log.info("[Global] 비즈니스 예외 발생 errorCode={}", e.getErrorCode());
		return ResponseEntity.status(e.getErrorCode().getHttpStatus())
				.body(CommonResponse.error(e.getErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
		log.info("[Global] 입력값 검증 실패 message={}", e.getMessage());
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
				.body(CommonResponse.error(ErrorCode.INVALID_REQUEST));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<CommonResponse<Void>> handleIllegalStateException(IllegalStateException e) {
		log.info("[Global] 잘못된 상태 요청 message={}", e.getMessage());
		return ResponseEntity.status(ErrorCode.INVALID_STATE.getHttpStatus())
				.body(CommonResponse.error(ErrorCode.INVALID_STATE));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
		log.error("[Global] 예상하지 못한 예외 발생", e);
		return ResponseEntity.internalServerError()
				.body(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
