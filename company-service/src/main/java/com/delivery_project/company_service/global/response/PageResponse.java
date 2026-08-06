package com.delivery_project.company_service.global.response;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(

        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> of(
            List<E> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            Function<E, T> mapper
    ) {
        return new PageResponse<>(
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
