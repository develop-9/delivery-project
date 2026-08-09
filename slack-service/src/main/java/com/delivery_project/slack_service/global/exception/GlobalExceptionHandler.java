package com.delivery_project.slack_service.global.exception;

import com.delivery_project.slack_service.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> createResponse(
            ErrorCode errorCode
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        log.info(
                "[BusinessException] errorCode={}",
                exception.getErrorCode()
        );

        return createResponse(
                exception.getErrorCode()
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(
            NoSuchElementException exception
    ) {
        log.warn(
                "[NoSuchElementException] {}",
                exception.getMessage()
        );

        return createResponse(
                ErrorCode.NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException exception
    ) {
        log.warn(
                "[IllegalStateException] {}",
                exception.getMessage()
        );

        return createResponse(
                ErrorCode.INVALID_STATE
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        log.warn(
                "[MethodArgumentNotValidException] {}",
                exception.getMessage()
        );

        return createResponse(
                ErrorCode.INVALID_INPUT_VALUE
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(
            Exception exception
    ) {
        log.warn(
                "[InvalidRequestException] {}",
                exception.getMessage()
        );

        return createResponse(
                ErrorCode.INVALID_REQUEST
        );
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(
            Exception exception
    ) {
        log.warn(
                "[InvalidRequest] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            Exception exception
    ) {
        log.info(
                "[AccessDeniedException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.AUTH_FORBIDDEN);
    }

    // HTTP 요청은 필터체인이 먼저 401을 내므로 여기까지 오지 않는다.
    // 필터를 거치지 않고 서비스를 직접 호출하는 경로(예: 스케줄러)에서만 발생한다.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception
    ) {
        log.info(
                "[AuthenticationException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception
    ) {
        log.warn(
                "[NoResourceFoundException] path={}",
                exception.getResourcePath()
        );

        return createResponse(
                ErrorCode.NOT_FOUND
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception
    ) {
        log.error(
                "[Exception]",
                exception
        );

        return createResponse(
                ErrorCode.INTERNAL_SERVER_ERROR
        );
    }
}