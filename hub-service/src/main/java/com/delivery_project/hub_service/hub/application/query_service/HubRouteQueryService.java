package com.delivery_project.hub_service.hub.application.query_service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.hub_service.global.config.CacheConfig;
import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.HubPathCalculator;
import com.delivery_project.hub_service.hub.application.HubSegmentPair;
import com.delivery_project.hub_service.hub.application.query.HubPathQuery;
import com.delivery_project.hub_service.hub.application.query.HubRouteGetQuery;
import com.delivery_project.hub_service.hub.application.query.HubRouteSearchQuery;
import com.delivery_project.hub_service.hub.application.result.HubPathResult;
import com.delivery_project.hub_service.hub.application.result.HubPathSegmentResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteDetailResult;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.repository.HubQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteSearchCondition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이동정보 조회와 경로 산출 (02_hub-routes.md 7·8·11번, 03_internal.md 14번).
 *
 * <p>경로 조회는 외부에 의존하지 않는다 — 지도 API 는 생성·수정에서만 쓰므로
 * 지도 API 가 죽어도 경로 조회와 주문 생성은 정상 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteQueryService {

	private final HubRouteQueryRepository hubRouteQueryRepository;
	private final HubQueryRepository hubQueryRepository;
	private final HubPathCalculator hubPathCalculator;

	public HubRouteDetailResult getHubRoute(HubRouteGetQuery query) {
		HubRoute hubRoute = hubRouteQueryRepository.findById(query.hubRouteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));

		Map<UUID, String> hubNames = resolveHubNames(
				Set.of(hubRoute.getDepartureHubId(), hubRoute.getArrivalHubId()));

		return toDetailResult(hubRoute, hubNames);
	}

	public Page<HubRouteDetailResult> searchHubRoutes(HubRouteSearchQuery query, Pageable pageable) {
		validateRanges(query);
		validateHubExists(query.departureHubId());
		validateHubExists(query.arrivalHubId());

		Page<HubRoute> hubRoutes = hubRouteQueryRepository.search(toSearchCondition(query), pageable);
		Map<UUID, String> hubNames = resolveHubNames(collectHubIds(hubRoutes.getContent()));

		return hubRoutes.map(hubRoute -> toDetailResult(hubRoute, hubNames));
	}

	/**
	 * 허브 간 경로 산출 (D2).
	 *
	 * <p>계층 규칙으로 구간을 쪼갠 뒤 각 구간의 거리·시간을 {@code p_hub_routes} 에서 읽는다.
	 * 하나라도 없으면 {@code 404 HUB_ROUTE_PATH_NOT_FOUND} 다 —
	 * <b>허브는 멀쩡한데 사이를 잇는 간선이 없는</b> 상태라 {@code HUB_NOT_FOUND} 와 원인이 다르다.
	 *
	 * <p>캐시 키는 규약이 정한 {@code hubPath:{depId}:{arrId}} 그대로다. 파라미터를 Query 로 묶은 뒤에도
	 * <b>만들어지는 문자열이 달라지면 안 되므로</b> 키 표현식만 {@code #query.xxx()} 로 바꿨다.
	 */
	@Cacheable(cacheNames = CacheConfig.HUB_PATH_CACHE,
			key = "#query.departureHubId().toString() + ':' + #query.arrivalHubId().toString()")
	public HubPathResult findPath(HubPathQuery query) {
		if (query.departureHubId().equals(query.arrivalHubId())) {
			throw new BusinessException(ErrorCode.SAME_HUB_NOT_ALLOWED);
		}

		Hub departure = loadHub(query.departureHubId());
		Hub arrival = loadHub(query.arrivalHubId());

		List<HubSegmentPair> pairs = hubPathCalculator.calculate(departure, arrival);
		List<HubRoute> segments = loadSegments(pairs);

		Map<UUID, String> hubNames = resolveHubNames(collectHubIds(segments));

		return toPathResult(departure, arrival, segments, hubNames);
	}

	private List<HubRoute> loadSegments(List<HubSegmentPair> pairs) {
		List<HubRoute> segments = new ArrayList<>(pairs.size());

		for (HubSegmentPair pair : pairs) {
			HubRoute segment = hubRouteQueryRepository
					.findSegment(pair.departureHubId(), pair.arrivalHubId())
					.orElseThrow(() -> {
						log.warn("[HubRoute] 경로 구간의 이동정보가 없다 departureHubId={} arrivalHubId={}",
								pair.departureHubId(), pair.arrivalHubId());
						return new BusinessException(ErrorCode.HUB_ROUTE_PATH_NOT_FOUND);
					});

			segments.add(segment);
		}

		return segments;
	}

	private HubPathResult toPathResult(Hub departure, Hub arrival, List<HubRoute> segments,
			Map<UUID, String> hubNames) {

		List<HubPathSegmentResult> segmentResults = new ArrayList<>(segments.size());
		BigDecimal totalDistanceKm = BigDecimal.ZERO;
		int totalDurationMin = 0;

		for (int index = 0; index < segments.size(); index++) {
			HubRoute segment = segments.get(index);

			// sequence 는 1부터. Delivery Service 가 p_delivery_routes.sequence 에 그대로 쓴다.
			segmentResults.add(HubPathSegmentResult.from(
					index + 1,
					segment,
					hubNames.get(segment.getDepartureHubId()),
					hubNames.get(segment.getArrivalHubId())
			));

			totalDistanceKm = totalDistanceKm.add(segment.getDistanceKm());
			totalDurationMin += segment.getDurationMin();
		}

		return new HubPathResult(
				departure.getId(),
				departure.getName(),
				arrival.getId(),
				arrival.getName(),
				totalDistanceKm,
				totalDurationMin,
				segmentResults.size(),
				toWaypointHubNames(segmentResults),
				segmentResults
		);
	}

	/**
	 * 경유 허브명 — 출발·도착을 뺀 가운데 허브들.
	 *
	 * <p>구간 {@code i} 의 도착 허브가 구간 {@code i+1} 의 출발 허브이므로
	 * <b>마지막 구간을 뺀</b> 나머지 구간의 도착 허브가 곧 경유지다.
	 */
	private List<String> toWaypointHubNames(List<HubPathSegmentResult> segments) {
		if (segments.size() <= 1) {
			return List.of();
		}

		return segments.subList(0, segments.size() - 1).stream()
				.map(HubPathSegmentResult::arrivalHubName)
				.toList();
	}

	private HubRouteDetailResult toDetailResult(HubRoute hubRoute, Map<UUID, String> hubNames) {
		return HubRouteDetailResult.from(
				hubRoute,
				hubNames.get(hubRoute.getDepartureHubId()),
				hubNames.get(hubRoute.getArrivalHubId())
		);
	}

	private Set<UUID> collectHubIds(List<HubRoute> hubRoutes) {
		Set<UUID> hubIds = new HashSet<>();

		for (HubRoute hubRoute : hubRoutes) {
			hubIds.add(hubRoute.getDepartureHubId());
			hubIds.add(hubRoute.getArrivalHubId());
		}

		return hubIds;
	}

	/** 구간마다 허브를 따로 읽으면 N+1 이 된다. 한 번에 모아 읽고 이름만 뽑아 쓴다. */
	private Map<UUID, String> resolveHubNames(Set<UUID> hubIds) {
		if (hubIds.isEmpty()) {
			return Map.of();
		}

		return hubQueryRepository.findAllByIds(hubIds).stream()
				.collect(Collectors.toMap(Hub::getId, Hub::getName, (left, right) -> left));
	}

	/**
	 * Query 를 리포지토리 포트의 입력 계약으로 옮긴다.
	 *
	 * <p>{@code HubRouteSearchCondition} 은 {@code domain/repository} 의 계약이라 presentation 이
	 * 직접 만들지 않는다. 변환은 검증을 마친 이 계층에서만 일어난다.
	 */
	private HubRouteSearchCondition toSearchCondition(HubRouteSearchQuery query) {
		return new HubRouteSearchCondition(
				query.departureHubId(),
				query.arrivalHubId(),
				query.minDurationMin(),
				query.maxDurationMin(),
				query.minDistanceKm(),
				query.maxDistanceKm()
		);
	}

	/** 소요 시간·거리 모두 같은 규칙이라 검증을 하나로 모았다. */
	private void validateRanges(HubRouteSearchQuery query) {
		validateRange(query.minDurationMin(), query.maxDurationMin(), 0);
		validateRange(query.minDistanceKm(), query.maxDistanceKm(), BigDecimal.ZERO);
	}

	/**
	 * 음수와 역전({@code max < min})을 {@code 400 INVALID_INPUT_VALUE} 로 막는다.
	 *
	 * <p>둘 중 하나만 주는 것은 정상이다 — 열린 범위로 본다. 0 을 타입마다 다르게 표현해야 해서
	 * ({@code 0} vs {@link BigDecimal#ZERO}) 기준값을 인자로 받는다.
	 */
	private <T extends Comparable<T>> void validateRange(T min, T max, T zero) {
		boolean negative = (min != null && min.compareTo(zero) < 0)
				|| (max != null && max.compareTo(zero) < 0);
		boolean inverted = min != null && max != null && max.compareTo(min) < 0;

		if (negative || inverted) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	/** 없는 허브로 필터링하면 빈 목록이 아니라 404 다 (02_hub-routes.md 8번). */
	private void validateHubExists(UUID hubId) {
		if (hubId != null) {
			loadHub(hubId);
		}
	}

	private Hub loadHub(UUID hubId) {
		return hubQueryRepository.findById(hubId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
	}
}
