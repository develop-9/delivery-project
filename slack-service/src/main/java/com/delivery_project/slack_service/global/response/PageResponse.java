package com.delivery_project.slack_service.global.response;

import com.delivery_project.slack_service.global.common.PageData;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <S, T> PageResponse<T> from(
            PageData<S> pageData,
            Function<S, T> mapper
    ) {
        return new PageResponse<>(
                pageData.content()
                        .stream()
                        .map(mapper)
                        .toList(),
                pageData.page(),
                pageData.size(),
                pageData.totalElements(),
                pageData.totalPages()
        );
    }
}