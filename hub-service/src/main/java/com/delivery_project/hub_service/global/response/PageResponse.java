package com.delivery_project.hub_service.global.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

/**
 * 공통 페이징 응답 (00_common.md).
 *
 * <p>{@code sort} 는 Spring 의 {@link Sort} 객체가 아니라 {@code "createdAt,desc"} 형태의
 * 문자열이다. 규약이 정한 응답 형태이기도 하고, {@link Sort} 를 그대로 직렬화하면
 * 내부 구조가 그대로 노출돼 라이브러리 버전에 따라 응답이 바뀐다.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		String sort
) {
	public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				formatSort(page.getSort())
		);
	}

	private static String formatSort(Sort sort) {
		return sort.stream()
				.map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
				.reduce((left, right) -> left + ";" + right)
				.orElse("");
	}
}
