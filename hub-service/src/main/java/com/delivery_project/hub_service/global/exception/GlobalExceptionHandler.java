package com.delivery_project.hub_service.global.exception;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
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

	/**
	 * 인증 주체가 없는 채로 {@code @PreAuthorize} 가 걸린 메서드에 들어온 경우
	 * ({@code AuthenticationCredentialsNotFoundException}).
	 *
	 * <p>HTTP 요청은 필터체인이 먼저 401 을 내므로 여기까지 오지 않는다. 스케줄러처럼
	 * <b>필터를 거치지 않고 서비스를 직접 부르는 경로</b>에서만 발생하는데, 이 핸들러가 없으면
	 * 아래 {@code Exception} 캐치올이 잡아 500 이 된다. 원인은 인증 부재이므로 401 로 맞춘다.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
		log.info("[Global] 인증 정보 없음 message={}", e.getMessage());
		return createResponse(ErrorCode.AUTH_UNAUTHORIZED);
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

	/**
	 * DB 제약 위반. 정상 경로에서는 Service 가 미리 검사해 문서에 정의된 코드
	 * ({@code DUPLICATE_HUB_NAME} 등)로 응답하므로, 여기까지 오는 건 검사와 저장 사이에
	 * 다른 요청이 끼어든 경합뿐이라 드물다.
	 *
	 * <p>어떤 제약이 깨졌는지까지 구분하지 않는다. 구분하려면 제약 이름을 예외 메시지에서
	 * 문자열로 파싱해야 하는데, DB·드라이버 버전에 따라 메시지가 바뀌면 조용히 깨진다.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
		log.warn("[Global] 데이터 무결성 제약 위반 message={}", e.getMessage());
		return createResponse(ErrorCode.INVALID_STATE);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
			HttpMediaTypeNotSupportedException e) {
		log.info("[Global] 지원하지 않는 미디어 타입 message={}", e.getMessage());
		return createResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * 매핑되지 않은 경로. 이 핸들러가 없으면 아래 Exception 캐치올이 먼저 잡아서
	 * Spring 이 스스로 냈을 404 를 500 으로 바꿔버린다.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
		log.info("[Global] 매핑되지 않은 경로 요청 path={}", e.getResourcePath());
		return createResponse(ErrorCode.NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("[Global] 예상하지 못한 예외 발생", e);
		return createResponse(ErrorCode.INTERNAL_SERVER_ERROR);
	}
}
