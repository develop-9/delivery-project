package com.delivery_project.hub_service.global.util;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 공통 페이징 규약(00_common.md)을 {@link Pageable} 로 옮긴다.
 *
 * <p>규약이 <b>잘못된 값을 에러로 만들지 않는다.</b> {@code size} 가 10·30·50 이 아니면 400 이 아니라
 * 10 으로 보정하고, 허용되지 않은 {@code sort}·{@code direction} 도 기본값으로 되돌린다.
 * 목록 조회는 결과 0건조차 정상 응답인 API 라, 파라미터 하나 때문에 조회가 실패하지 않는 쪽으로 맞췄다.
 *
 * <p>Spring 의 {@code @PageableDefault} 를 쓰지 않는 이유는 규약이 정렬을
 * {@code sort=createdAt,desc} 한 파라미터가 아니라 {@code sort} + {@code direction} 두 개로
 * 나눠 받기로 했기 때문이다.
 */
public final class PageableFactory {

	private static final int DEFAULT_SIZE = 10;
	private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

	private static final String DEFAULT_SORT = "createdAt";
	private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt");

	private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;

	private PageableFactory() {
	}

	public static Pageable of(Integer page, Integer size, String sort, String direction) {
		return PageRequest.of(
				resolvePage(page),
				resolveSize(size),
				Sort.by(resolveDirection(direction), resolveSort(sort))
		);
	}

	private static int resolvePage(Integer page) {
		return page == null || page < 0 ? 0 : page;
	}

	private static int resolveSize(Integer size) {
		return size != null && ALLOWED_SIZES.contains(size) ? size : DEFAULT_SIZE;
	}

	private static String resolveSort(String sort) {
		return sort != null && ALLOWED_SORTS.contains(sort) ? sort : DEFAULT_SORT;
	}

	private static Sort.Direction resolveDirection(String direction) {
		if (direction == null) {
			return DEFAULT_DIRECTION;
		}
		return Sort.Direction.fromOptionalString(direction).orElse(DEFAULT_DIRECTION);
	}
}
