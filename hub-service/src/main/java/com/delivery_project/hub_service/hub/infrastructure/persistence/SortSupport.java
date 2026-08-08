package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;

/**
 * {@link Sort} 를 QueryDSL {@link OrderSpecifier} 로 옮긴다.
 *
 * <p>정렬 가능한 프로퍼티는 여기서 고정하지 않고 호출하는 도메인이 {@code sortablePaths} 로 정한다.
 * 감사 필드만 쓰는 도메인도 있고 도메인 고유 필드를 얹는 도메인도 있어서다.
 * 어떤 값을 받아줄지는 {@code PageableFactory} 가 이미 걸러주므로, 여기서는 매핑만 본다.
 *
 * <p><b>매핑에 없는 프로퍼티는 조용히 무시한다</b> — 규약이 {@code size} 를 에러 대신 10 으로 보정하듯,
 * 정렬도 잘못된 값 때문에 조회가 실패하지 않는 쪽으로 맞췄다.
 * 남는 것이 없으면 {@code defaultPath} 의 내림차순을 쓴다.
 */
final class SortSupport {

	private SortSupport() {
	}

	/**
	 * 시각·숫자 경로를 한 매핑에 담아야 해서 공통 상위 타입인 {@link ComparableExpressionBase} 를 받는다.
	 */
	static OrderSpecifier<?>[] toOrderSpecifiers(Sort sort,
			Map<String, ComparableExpressionBase<?>> sortablePaths,
			ComparableExpressionBase<?> defaultPath) {

		List<OrderSpecifier<?>> specifiers = new ArrayList<>();

		for (Sort.Order order : sort) {
			ComparableExpressionBase<?> path = sortablePaths.get(order.getProperty());

			if (path != null) {
				specifiers.add(new OrderSpecifier<>(order.isAscending() ? Order.ASC : Order.DESC, path));
			}
		}

		if (specifiers.isEmpty()) {
			specifiers.add(defaultPath.desc());
		}

		return specifiers.toArray(OrderSpecifier[]::new);
	}
}
