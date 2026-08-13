package com.delivery_project.hub_service.hub.infrastructure.persistence;

import static com.delivery_project.hub_service.hub.domain.entity.QHub.hub;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.domain.repository.HubQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 조회는 조건이 전부 선택적이라 파생 쿼리로는 표현이 안 된다. 컨벤션 §10 대로 QueryDSL 을 쓴다.
 *
 * <p>{@code deleted_at IS NULL} 은 {@code Hub} 의 {@code @SQLRestriction} 이 붙여준다.
 * QueryDSL 이 만든 JPQL 에도 적용되므로 여기서 조건을 쓰지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class HubQueryRepositoryImpl implements HubQueryRepository {

	private static final Map<String, ComparableExpressionBase<?>> SORTABLE_PATHS = Map.of(
			"createdAt", hub.createdAt,
			"updatedAt", hub.updatedAt
	);

	private final SpringDataHubRepository springDataHubRepository;
	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Hub> findById(UUID hubId) {
		return springDataHubRepository.findById(hubId);
	}

	@Override
	public List<Hub> findAllByIds(Collection<UUID> hubIds) {
		return springDataHubRepository.findByIdIn(hubIds);
	}

	/**
	 * ID 컬럼만 프로젝션한다. {@code findAll()} 로 엔티티를 다 읽어 ID 만 뽑으면 쓰지 않을 객체가
	 * 영속성 컨텍스트에 올라간다.
	 *
	 * <p>정렬을 걸지 않는다 — 호출 측은 ID 집합만 쓰고 순서에 의미가 없다 (03_internal.md 15번).
	 */
	@Override
	public List<UUID> findAllIds() {
		return queryFactory
				.select(hub.id)
				.from(hub)
				.fetch();
	}

	@Override
	public Page<Hub> search(HubSearchCondition condition, Pageable pageable) {
		List<Hub> content = queryFactory
				.selectFrom(hub)
				.where(
						keywordContains(condition.keyword()),
						hubTypeEquals(condition.hubType()),
						parentHubIdEquals(condition.parentHubId())
				)
				.orderBy(SortSupport.toOrderSpecifiers(pageable.getSort(), SORTABLE_PATHS, hub.createdAt))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		JPAQuery<Long> countQuery = queryFactory
				.select(hub.count())
				.from(hub)
				.where(
						keywordContains(condition.keyword()),
						hubTypeEquals(condition.hubType()),
						parentHubIdEquals(condition.parentHubId())
				);

		// 마지막 페이지이거나 첫 페이지가 다 안 찼으면 count 쿼리를 생략한다.
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	/** 허브명 또는 주소 부분 일치. */
	private BooleanExpression keywordContains(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}
		return hub.name.containsIgnoreCase(keyword).or(hub.address.containsIgnoreCase(keyword));
	}

	private BooleanExpression hubTypeEquals(HubType hubType) {
		return hubType == null ? null : hub.hubType.eq(hubType);
	}

	/** D1 때문에 중앙 허브 자신도 걸린다. 자신을 빼려면 호출자가 {@code hubType = SUB} 를 함께 준다. */
	private BooleanExpression parentHubIdEquals(UUID parentHubId) {
		return parentHubId == null ? null : hub.parentHubId.eq(parentHubId);
	}
}
