package com.delivery_project.user_service.global.exception;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.delivery_project.user_service.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private ResponseEntity<ErrorResponse> createResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ErrorResponse.from(errorCode));
	}

	private ResponseEntity<ErrorResponse> createResponse(ErrorCode errorCode, List<FieldErrorDto> fieldErrors) {
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ErrorResponse.from(errorCode, fieldErrors));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		log.info("[Global] 비즈니스 예외 발생 errorCode={}", e.getErrorCode());
		return createResponse(e.getErrorCode());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
		log.info("[Global] 잘못된 상태 요청 message={}", e.getMessage());
		return createResponse(ErrorCode.INVALID_STATE);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
		log.info("[Global] 리소스 조회 실패 message={}", e.getMessage());
		return createResponse(ErrorCode.NOT_FOUND);
	}

	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception e) {
		log.info("[Global] 접근 권한 없음 message={}", e.getMessage());
		return createResponse(ErrorCode.AUTH_FORBIDDEN);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
		List<FieldErrorDto> fieldErrors = e.getFieldErrors().stream()
				.map(FieldErrorDto::from)
				.toList();
		log.info("[Global] 입력값 검증 실패 fieldErrors={}", fieldErrors);
		return createResponse(ErrorCode.INVALID_INPUT_VALUE, fieldErrors);
	}

	@ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ErrorResponse> handleInvalidInputValueException(Exception e) {
		log.info("[Global] 입력값 검증 실패 message={}", e.getMessage());
		return createResponse(ErrorCode.INVALID_INPUT_VALUE);
	}

	@ExceptionHandler({
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequestException(Exception e) {
		log.info("[Global] 잘못된 요청 message={}", e.getMessage());
		return createResponse(ErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
		log.warn("[Global] 데이터 무결성 제약 위반 message={}", e.getMessage());
		return createResponse(ErrorCode.INVALID_STATE);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
		log.info("[Global] 지원하지 않는 미디어 타입 message={}", e.getMessage());
		return createResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("[Global] 예상하지 못한 예외 발생", e);
		return createResponse(ErrorCode.INTERNAL_SERVER_ERROR);
	}
}
