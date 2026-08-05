package com.delivery_project.hub_service.global.exception;

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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.delivery_project.hub_service.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private ResponseEntity<ErrorResponse> createResponse(
			ErrorCode code
	) {
		return ResponseEntity.status(code.getStatus())
				.body(ErrorResponse.from(code));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		log.error("[BusinessException] = {}", e.getMessage());
		ErrorCode code = e.getErrorCode();

		return createResponse(code);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
		log.error("[IllegalStateException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_STATE;

		return createResponse(code);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
		log.error("[NoSuchElementException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.NOT_FOUND;

		return createResponse(code);
	}

	/**
	 * DB 제약 위반. 없으면 아래 {@code Exception} 캐치올이 잡아 <b>500</b> 이 나간다.
	 *
	 * <p>정상 경로에서는 Service 가 미리 검사해 문서에 정의된 코드
	 * ({@code DUPLICATE_HUB_NAME} 등)로 응답한다. 여기까지 오는 건 <b>검사와 저장 사이에
	 * 다른 요청이 끼어든 경합</b>뿐이라 드물다.
	 *
	 * <p>그래서 어떤 제약이 깨졌는지까지 구분하지 않고 409 로만 답한다.
	 * 구분하려면 제약 이름을 예외 메시지에서 문자열로 파싱해야 하는데,
	 * DB·드라이버 버전에 따라 메시지가 바뀌면 조용히 깨지는 코드가 된다.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
			DataIntegrityViolationException e) {
		log.error("[DataIntegrityViolationException] = {}", e.getMostSpecificCause().getMessage());
		ErrorCode code = ErrorCode.INVALID_STATE;

		return createResponse(code);
	}

	/**
	 * 매핑되지 않은 경로. 아래 {@code Exception} 캐치올이 없었다면 Spring 이 스스로 404 를 냈겠지만,
	 * 캐치올이 먼저 잡아 500 으로 바꿔버리므로 여기서 되돌린다.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
		log.warn("[NoResourceFoundException] = {}", e.getResourcePath());
		ErrorCode code = ErrorCode.NOT_FOUND;

		return createResponse(code);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("[Exception] = {}", e.getMessage());
		log.error("[Exception] = {}", e.getClass().getSimpleName());
		ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

		return createResponse(code);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
			MissingServletRequestPartException e) {
		log.error("필수 파트 누락: {}", e.getRequestPartName());
		ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

		return createResponse(code);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException e) {
		log.warn("필수 쿼리 파라미터 누락: {}", e.getParameterName());
		ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

		return createResponse(code);
	}

	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	public ResponseEntity<ErrorResponse> handleAccessDenied(Exception e) {
		log.error("[AccessDenied] = {}", e.getMessage());
		ErrorCode code = ErrorCode.AUTH_FORBIDDEN;

		return createResponse(code);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
		log.error("[IllegalArgumentException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_REQUEST;

		return createResponse(code);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMisMatchException(
			MethodArgumentTypeMismatchException e) {
		log.error("[MethodArgumentTypeMismatchException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_REQUEST;

		return createResponse(code);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException e) {
		log.error("[MethodArgumentNotValidException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

		return createResponse(code);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
			HttpMessageNotReadableException e) {
		log.error("[HttpMessageNotReadableException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_REQUEST;

		return createResponse(code);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
			HttpMediaTypeNotSupportedException e) {
		log.error("[HttpMediaTypeNotSupportedException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.UNSUPPORTED_MEDIA_TYPE;

		return createResponse(code);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
		log.error("[BindException] = {}", e.getMessage());
		ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

		return createResponse(code);
	}
}
