package com.delivery_project.slack_service.global.common;

import java.util.List;
import java.util.function.Function;

public record PageData<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public <R> PageData<R> map(Function<T, R> mapper) {
        return new PageData<>(
                content.stream()
                        .map(mapper)
                        .toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}