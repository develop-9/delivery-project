package com.delivery_project.company_service.global.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class BusinessException extends RuntimeException {

    private final Instant timestamp;
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
    }
}
