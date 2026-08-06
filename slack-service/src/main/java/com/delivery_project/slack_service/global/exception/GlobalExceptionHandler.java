package com.delivery_project.slack_service.global.exception;

import com.delivery_project.slack_service.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

        return createResponse(exception.getErrorCode());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(
            NoSuchElementException exception
    ) {
        log.warn(
                "[NoSuchElementException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException exception
    ) {
        log.warn(
                "[IllegalStateException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.INVALID_STATE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        log.warn(
                "[IllegalArgumentException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        log.warn(
                "[MethodArgumentNotValidException] {}",
                exception.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception
    ) {
        log.info(
                "[NoResourceFoundException] path={}",
                exception.getResourcePath()
        );

        return createResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception
    ) {
        log.error("[Exception]", exception);

        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}