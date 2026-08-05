package com.delivery_project.slack_service.global.response;

public record SuccessResponse<T>(
        boolean success,
        T data
) {

    public static <T> SuccessResponse<T> success(T data) {
        return new SuccessResponse<>(true, data);
    }
}