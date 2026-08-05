package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.DateTimePath;

/**
 * {@link Sort} 를 QueryDSL {@link OrderSpecifier} 로 옮긴다.
 *
 * <p>정렬 허용값은 {@code createdAt} · {@code updatedAt} 둘뿐이다 (공통 페이징 규약).
 * <b>그 외 필드는 조용히 무시한다</b> — 규약이 {@code size} 를 에러 대신 10 으로 보정하듯,
 * 정렬도 잘못된 값 때문에 조회가 실패하지 않는 쪽으로 맞췄다.
 * 남는 것이 없으면 기본값 {@code createdAt desc} 를 쓴다.
 */
final class AuditSortSupport {

	private AuditSortSupport() {
	}

	static OrderSpecifier<?>[] toOrderSpecifiers(Sort sort, DateTimePath<Instant> createdAt,
			DateTimePath<Instant> updatedAt) {

		List<OrderSpecifier<?>> specifiers = new ArrayList<>();

		for (Sort.Order order : sort) {
			DateTimePath<Instant> path = switch (order.getProperty()) {
				case "createdAt" -> createdAt;
				case "updatedAt" -> updatedAt;
				default -> null;
			};

			if (path != null) {
				specifiers.add(new OrderSpecifier<>(order.isAscending() ? Order.ASC : Order.DESC, path));
			}
		}

		if (specifiers.isEmpty()) {
			specifiers.add(createdAt.desc());
		}

		return specifiers.toArray(OrderSpecifier[]::new);
	}
}
