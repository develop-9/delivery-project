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

	/**
	 * 모든 도메인이 공통으로 허용하는 감사 필드. 도메인은 여기에 자기 정렬 키를 얹기만 하면 된다.
	 */
	public static final Set<String> AUDIT_SORTS = Set.of("createdAt", "updatedAt");

	private static final int DEFAULT_SIZE = 10;
	private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

	private static final String DEFAULT_SORT = "createdAt";

	private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;

	private PageableFactory() {
	}

	public static Pageable of(Integer page, Integer size, String sort, String direction) {
		return of(page, size, sort, direction, null);
	}

	/**
	 * 도메인 고유 정렬 키를 추가로 허용한다. 감사 필드는 {@code allowedSorts} 와 무관하게 항상 허용된다.
	 */
	public static Pageable of(Integer page, Integer size, String sort, String direction,
			Set<String> allowedSorts) {
		return PageRequest.of(
				resolvePage(page),
				resolveSize(size),
				Sort.by(resolveDirection(direction), resolveSort(sort, allowedSorts))
		);
	}

	private static int resolvePage(Integer page) {
		return page == null || page < 0 ? 0 : page;
	}

	private static int resolveSize(Integer size) {
		return size != null && ALLOWED_SIZES.contains(size) ? size : DEFAULT_SIZE;
	}

	private static String resolveSort(String sort, Set<String> allowedSorts) {
		if (sort == null) {
			return DEFAULT_SORT;
		}
		if (AUDIT_SORTS.contains(sort)) {
			return sort;
		}
		return allowedSorts != null && allowedSorts.contains(sort) ? sort : DEFAULT_SORT;
	}

	private static Sort.Direction resolveDirection(String direction) {
		if (direction == null) {
			return DEFAULT_DIRECTION;
		}
		return Sort.Direction.fromOptionalString(direction).orElse(DEFAULT_DIRECTION);
	}
}
