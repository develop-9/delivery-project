package com.delivery_project.hub_service.hub.infrastructure.persistence;

import static com.delivery_project.hub_service.hub.domain.entity.QHubRoute.hubRoute;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HubRouteQueryRepositoryImpl implements HubRouteQueryRepository {

	/**
	 * 감사 필드에 더해 거리·시간으로도 정렬한다. 둘 다 범위 검색 대상이라 "가까운 순"·"빠른 순"이
	 * 자연스러운 후속 요구라서다. {@code NumberPath} 도 {@link ComparableExpressionBase} 하위라
	 * 시각 경로와 한 매핑에 담긴다.
	 */
	private static final Map<String, ComparableExpressionBase<?>> SORTABLE_PATHS = Map.of(
			"createdAt", hubRoute.createdAt,
			"updatedAt", hubRoute.updatedAt,
			"durationMin", hubRoute.durationMin,
			"distanceKm", hubRoute.distanceKm
	);

	private final SpringDataHubRouteRepository springDataHubRouteRepository;
	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<HubRoute> findById(UUID hubRouteId) {
		return springDataHubRouteRepository.findById(hubRouteId);
	}

	@Override
	public Optional<HubRoute> findSegment(UUID departureHubId, UUID arrivalHubId) {
		return springDataHubRouteRepository
				.findByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId);
	}

	@Override
	public Page<HubRoute> search(HubRouteSearchCondition condition, Pageable pageable) {
		List<HubRoute> content = queryFactory
				.selectFrom(hubRoute)
				.where(predicates(condition))
				.orderBy(SortSupport.toOrderSpecifiers(pageable.getSort(), SORTABLE_PATHS,
						hubRoute.createdAt))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		JPAQuery<Long> countQuery = queryFactory
				.select(hubRoute.count())
				.from(hubRoute)
				.where(predicates(condition));

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	/**
	 * 목록과 count 가 같은 조건을 봐야 한다. 조건이 여섯 개로 늘어난 뒤로는 두 번 나열하면
	 * 한쪽만 고치는 실수가 나기 쉬워 한곳에서 만든다. {@code null} 은 QueryDSL 이 무시한다.
	 */
	private BooleanExpression[] predicates(HubRouteSearchCondition condition) {
		return new BooleanExpression[] {
				departureHubIdEquals(condition.departureHubId()),
				arrivalHubIdEquals(condition.arrivalHubId()),
				durationMinGoe(condition.minDurationMin()),
				durationMinLoe(condition.maxDurationMin()),
				distanceKmGoe(condition.minDistanceKm()),
				distanceKmLoe(condition.maxDistanceKm())
		};
	}

	private BooleanExpression departureHubIdEquals(UUID departureHubId) {
		return departureHubId == null ? null : hubRoute.departureHubId.eq(departureHubId);
	}

	private BooleanExpression arrivalHubIdEquals(UUID arrivalHubId) {
		return arrivalHubId == null ? null : hubRoute.arrivalHubId.eq(arrivalHubId);
	}

	private BooleanExpression durationMinGoe(Integer minDurationMin) {
		return minDurationMin == null ? null : hubRoute.durationMin.goe(minDurationMin);
	}

	private BooleanExpression durationMinLoe(Integer maxDurationMin) {
		return maxDurationMin == null ? null : hubRoute.durationMin.loe(maxDurationMin);
	}

	private BooleanExpression distanceKmGoe(BigDecimal minDistanceKm) {
		return minDistanceKm == null ? null : hubRoute.distanceKm.goe(minDistanceKm);
	}

	private BooleanExpression distanceKmLoe(BigDecimal maxDistanceKm) {
		return maxDistanceKm == null ? null : hubRoute.distanceKm.loe(maxDistanceKm);
	}
}
