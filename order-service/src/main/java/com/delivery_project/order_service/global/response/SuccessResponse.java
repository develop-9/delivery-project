package com.delivery_project.order_service.global.response;

public record SuccessResponse<T>(
        boolean success,
        T data
) {
    public static <T> SuccessResponse<T> success(T data) {
        return new SuccessResponse<>(true, data);
    }

    /** 본문 없는 성공 응답. 레코드 컴포넌트 success 와 이름이 겹쳐 무인자 success() 는 못 만든다 */
    public static SuccessResponse<Void> empty() {
        return new SuccessResponse<>(true, null);
    }
}
