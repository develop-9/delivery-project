package com.delivery_project.hub_service.hub.infrastructure.persistence;

import static com.delivery_project.hub_service.hub.domain.entity.QHubRoute.hubRoute;

import java.util.List;
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
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaHubRouteQueryRepository implements HubRouteQueryRepository {

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
				.where(
						departureHubIdEquals(condition.departureHubId()),
						arrivalHubIdEquals(condition.arrivalHubId()),
						durationMinGoe(condition.minDurationMin()),
						durationMinLoe(condition.maxDurationMin())
				)
				.orderBy(AuditSortSupport.toOrderSpecifiers(pageable.getSort(), hubRoute.createdAt,
						hubRoute.updatedAt))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		JPAQuery<Long> countQuery = queryFactory
				.select(hubRoute.count())
				.from(hubRoute)
				.where(
						departureHubIdEquals(condition.departureHubId()),
						arrivalHubIdEquals(condition.arrivalHubId()),
						durationMinGoe(condition.minDurationMin()),
						durationMinLoe(condition.maxDurationMin())
				);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
}
