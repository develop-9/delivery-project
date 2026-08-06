package com.delivery_project.order_service.global.exception;

import com.delivery_project.order_service.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> createResponse(ErrorCode code) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.from(code));
    }

    private ResponseEntity<ErrorResponse> createResponse(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] code={}, message={}", code.getCode(), e.getMessage());

        // 예외가 상세 메시지를 들고 있으면 그것을 그대로 내려준다 (재고 부족 수량 등)
        return createResponse(code, e.getMessage());
    }

    /** 낙관적 락 충돌 — 동시에 같은 주문을 수정한 경우 */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("[OptimisticLock] {}", e.getMessage());

        return createResponse(ErrorCode.CONCURRENT_UPDATE_CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("[IllegalStateException] = {}", e.getMessage());

        return createResponse(ErrorCode.INVALID_STATE);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        log.error("[NoSuchElementException] = {}", e.getMessage());

        return createResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("[IllegalArgumentException] = {}", e.getMessage());

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    /** @Valid 검증 실패 — 첫 번째 필드 메시지를 그대로 내려준다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        log.warn("[MethodArgumentNotValidException] = {}", message);

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    /** 쿼리 파라미터 바인딩(검색 조건) 검증 실패 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        log.warn("[BindException] = {}", message);

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("필수 쿼리 파라미터 누락: {}", e.getParameterName());

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e) {
        log.warn("필수 파트 누락: {}", e.getRequestPartName());

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("[MethodArgumentTypeMismatchException] = {}", e.getMessage());

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] = {}", e.getMessage());

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e) {
        log.warn("[HttpMediaTypeNotSupportedException] = {}", e.getMessage());

        return createResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }


    //매핑되지 않은 경로(예: 경로변수가 빈 /api/v1/orders/).
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[NoResourceFoundException] 매핑되지 않은 경로: {}", e.getResourcePath());

        return createResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Exception] type={}, message={}", e.getClass().getSimpleName(), e.getMessage(), e);

        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
