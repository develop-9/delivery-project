package com.delivery_project.hub_service.hub.application.query_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.delivery_project.hub_service.global.config.CacheConfig;
import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.HubPathCalculator;
import com.delivery_project.hub_service.hub.application.query.HubPathQuery;
import com.delivery_project.hub_service.hub.application.query.HubRouteGetQuery;
import com.delivery_project.hub_service.hub.application.query.HubRouteSearchQuery;
import com.delivery_project.hub_service.hub.domain.repository.HubQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteSearchCondition;

/**
 * 이동정보 조회 단위 테스트.
 *
 * <p>검색은 Query 를 받아 검증한 뒤 리포지토리 포트의 입력 계약({@link HubRouteSearchCondition})으로
 * 옮기는 것이 서비스의 책임이라, 리포지토리에 <b>어떤 조건이 넘어갔는지</b>를 확인한다.
 * {@code null} 조건을 실제로 무시하는 것은 QueryDSL 쪽 계약이다.
 */
@ExtendWith(MockitoExtension.class)
class HubRouteQueryServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 10);

	@Mock
	private HubRouteQueryRepository hubRouteQueryRepository;

	@Mock
	private HubQueryRepository hubQueryRepository;

	@Mock
	private HubPathCalculator hubPathCalculator;

	@InjectMocks
	private HubRouteQueryService hubRouteQueryService;

	@Captor
	private ArgumentCaptor<HubRouteSearchCondition> conditionCaptor;

	@Nested
	@DisplayName("거리 범위 검색")
	class DistanceRange {

		@Test
		@DisplayName("최소 거리만 주면 하한만 조건에 담긴다")
		void passesMinDistanceOnly() {
			// given
			stubEmptySearch();

			HubRouteSearchQuery query = searchQuery(BigDecimal.valueOf(50.00), null);

			// when
			hubRouteQueryService.searchHubRoutes(query, PAGEABLE);

			// then
			HubRouteSearchCondition condition = capturedCondition();
			assertThat(condition.minDistanceKm()).isEqualByComparingTo("50.00");
			assertThat(condition.maxDistanceKm()).isNull();
		}

		@Test
		@DisplayName("최대 거리만 주면 상한만 조건에 담긴다")
		void passesMaxDistanceOnly() {
			// given
			stubEmptySearch();

			HubRouteSearchQuery query = searchQuery(null, BigDecimal.valueOf(200.00));

			// when
			hubRouteQueryService.searchHubRoutes(query, PAGEABLE);

			// then
			HubRouteSearchCondition condition = capturedCondition();
			assertThat(condition.minDistanceKm()).isNull();
			assertThat(condition.maxDistanceKm()).isEqualByComparingTo("200.00");
		}

		@Test
		@DisplayName("둘 다 주면 범위가 그대로 조건에 담긴다")
		void passesDistanceRange() {
			// given
			stubEmptySearch();

			HubRouteSearchQuery query = searchQuery(BigDecimal.valueOf(50.00), BigDecimal.valueOf(200.00));

			// when
			hubRouteQueryService.searchHubRoutes(query, PAGEABLE);

			// then
			HubRouteSearchCondition condition = capturedCondition();
			assertThat(condition.minDistanceKm()).isEqualByComparingTo("50.00");
			assertThat(condition.maxDistanceKm()).isEqualByComparingTo("200.00");
		}

		@Test
		@DisplayName("둘 다 주지 않으면 거리 조건이 비어 무시된다")
		void ignoresDistanceWhenAbsent() {
			// given
			stubEmptySearch();

			HubRouteSearchQuery query = searchQuery(null, null);

			// when
			hubRouteQueryService.searchHubRoutes(query, PAGEABLE);

			// then
			HubRouteSearchCondition condition = capturedCondition();
			assertThat(condition.minDistanceKm()).isNull();
			assertThat(condition.maxDistanceKm()).isNull();
		}

		@Test
		@DisplayName("거리가 음수면 INVALID_INPUT_VALUE 다")
		void rejectsNegativeDistance() {
			// given
			HubRouteSearchQuery query = searchQuery(BigDecimal.valueOf(-1.00), null);

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.searchHubRoutes(query, PAGEABLE))
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
			verify(hubRouteQueryRepository, never()).search(any(), any());
		}

		@Test
		@DisplayName("최대 거리가 최소 거리보다 작으면 INVALID_INPUT_VALUE 다")
		void rejectsInvertedDistanceRange() {
			// given
			HubRouteSearchQuery query = searchQuery(BigDecimal.valueOf(200.00), BigDecimal.valueOf(50.00));

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.searchHubRoutes(query, PAGEABLE))
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
			verify(hubRouteQueryRepository, never()).search(any(), any());
		}
	}

	@Nested
	@DisplayName("소요 시간 범위 검색")
	class DurationRange {

		@Test
		@DisplayName("소요 시간이 음수면 INVALID_INPUT_VALUE 다")
		void rejectsNegativeDuration() {
			// given
			HubRouteSearchQuery query = new HubRouteSearchQuery(null, null, -1, null, null, null);

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.searchHubRoutes(query, PAGEABLE))
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
		}

		@Test
		@DisplayName("최대 소요 시간이 최소보다 작으면 INVALID_INPUT_VALUE 다")
		void rejectsInvertedDurationRange() {
			// given
			HubRouteSearchQuery query = new HubRouteSearchQuery(null, null, 90, 30, null, null);

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.searchHubRoutes(query, PAGEABLE))
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
		}

		@Test
		@DisplayName("소요 시간과 거리 범위를 함께 주면 네 조건이 모두 담긴다")
		void passesBothRanges() {
			// given
			stubEmptySearch();

			HubRouteSearchQuery query = new HubRouteSearchQuery(
					null, null, 30, 90, BigDecimal.valueOf(50.00), BigDecimal.valueOf(200.00));

			// when
			hubRouteQueryService.searchHubRoutes(query, PAGEABLE);

			// then
			HubRouteSearchCondition condition = capturedCondition();
			assertThat(condition.minDurationMin()).isEqualTo(30);
			assertThat(condition.maxDurationMin()).isEqualTo(90);
			assertThat(condition.minDistanceKm()).isEqualByComparingTo("50.00");
			assertThat(condition.maxDistanceKm()).isEqualByComparingTo("200.00");
		}
	}

	@Nested
	@DisplayName("Query 로 받은 값이 그대로 쓰인다")
	class QueryWiring {

		@Test
		@DisplayName("없는 이동정보를 조회하면 HUB_ROUTE_NOT_FOUND 다")
		void rejectsMissingHubRoute() {
			// given
			UUID hubRouteId = UUID.randomUUID();
			when(hubRouteQueryRepository.findById(hubRouteId)).thenReturn(Optional.empty());

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.getHubRoute(new HubRouteGetQuery(hubRouteId)))
					.isEqualTo(ErrorCode.HUB_ROUTE_NOT_FOUND);
		}

		@Test
		@DisplayName("출발과 도착이 같은 경로 조회는 SAME_HUB_NOT_ALLOWED 다")
		void rejectsSameHubPath() {
			// given
			UUID hubId = UUID.randomUUID();

			// when & then
			assertThatErrorCode(() -> hubRouteQueryService.findPath(new HubPathQuery(hubId, hubId)))
					.isEqualTo(ErrorCode.SAME_HUB_NOT_ALLOWED);
		}
	}

	@Nested
	@DisplayName("경로 캐시 키")
	class PathCacheKey {

		/**
		 * 파라미터를 Query 로 묶으면서 키 표현식을 {@code #query.xxx()} 로 바꿨다.
		 * 만들어지는 문자열은 규약이 정한 {@code hubPath:{depId}:{arrId}} 그대로여야 한다.
		 */
		@Test
		@DisplayName("Query 로 바꿔도 hubPath:{depId}:{arrId} 그대로 만들어진다")
		void keepsKeyFormat() throws Exception {
			// given
			UUID departureHubId = UUID.randomUUID();
			UUID arrivalHubId = UUID.randomUUID();

			Method findPath = HubRouteQueryService.class.getMethod("findPath", HubPathQuery.class);
			Cacheable cacheable = findPath.getAnnotation(Cacheable.class);

			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setVariable("query", new HubPathQuery(departureHubId, arrivalHubId));

			// when
			String key = new SpelExpressionParser()
					.parseExpression(cacheable.key())
					.getValue(context, String.class);

			// then
			assertThat(cacheable.cacheNames()).containsExactly(CacheConfig.HUB_PATH_CACHE);
			assertThat(key).isEqualTo(departureHubId + ":" + arrivalHubId);
		}
	}

	private void stubEmptySearch() {
		when(hubRouteQueryRepository.search(any(), any())).thenReturn(Page.empty(PAGEABLE));
	}

	private HubRouteSearchCondition capturedCondition() {
		verify(hubRouteQueryRepository).search(conditionCaptor.capture(), any());
		return conditionCaptor.getValue();
	}

	private static HubRouteSearchQuery searchQuery(BigDecimal minDistanceKm, BigDecimal maxDistanceKm) {
		return new HubRouteSearchQuery(null, null, null, null, minDistanceKm, maxDistanceKm);
	}

	private static AbstractObjectAssert<?, ErrorCode> assertThatErrorCode(ThrowingCallable callable) {
		return assertThat(errorCodeOf(callable));
	}

	private static ErrorCode errorCodeOf(ThrowingCallable callable) {
		try {
			callable.call();
		} catch (BusinessException e) {
			return e.getErrorCode();
		} catch (Throwable t) {
			throw new AssertionError(
					"BusinessException 이 아니라 " + t.getClass().getSimpleName() + " 가 던져졌다", t);
		}

		throw new AssertionError("예외가 던져지지 않았다");
	}
}
