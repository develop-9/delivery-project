package com.delivery_project.hub_service.hub.application.command_service;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.global.util.CacheEvictor;
import com.delivery_project.hub_service.hub.application.command.HubRouteCreateCommand;
import com.delivery_project.hub_service.hub.application.command.HubRouteUpdateCommand;
import com.delivery_project.hub_service.hub.application.result.HubRouteCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteUpdateResult;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.repository.HubRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이동정보 생성·수정·삭제 (02_hub-routes.md 6·9·10번).
 *
 * <p><b>거리·시간은 요청이 준 값을 그대로 쓴다.</b> 문서의 D8 은 지도 API 자동 산출을 정하고 있으나
 * 연동은 아직 넣지 않았다. 그래서 생성은 두 값을 필수로 받고, 수정은 보낸 값만 바꾼다.
 *
 * <p>문서상 권한은 "MASTER · 스케줄러"지만 {@code @PreAuthorize} 는 {@code MASTER} 만 건다.
 * 스케줄러는 HTTP 를 타지 않고 이 서비스를 직접 부르는 경로가 될 예정이라 인증 주체가 없다.
 * <b>TODO</b> 스케줄러가 들어올 때 시스템 주체로 인증 컨텍스트를 채울지, 별도 진입점을 둘지 정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HubRouteCommandService {

	private final HubRouteRepository hubRouteRepository;
	private final HubRepository hubRepository;
	private final CacheEvictor cacheEvictor;

	@PreAuthorize("hasRole('MASTER')")
	public HubRouteCreateResult create(UUID callerId, HubRouteCreateCommand command) {
		log.info("[HubRoute] 이동정보 생성 요청 departureHubId={} arrivalHubId={} callerId={}",
				command.departureHubId(), command.arrivalHubId(), callerId);

		validateDifferentHubs(command.departureHubId(), command.arrivalHubId());

		Hub departure = loadHub(command.departureHubId());
		Hub arrival = loadHub(command.arrivalHubId());

		validateSegment(departure, arrival);
		validateNotDuplicated(departure.getId(), arrival.getId());

		HubRoute saved = hubRouteRepository.save(HubRoute.create(
				departure.getId(), arrival.getId(), command.distanceKm(), command.durationMin()));

		cacheEvictor.evictAllHubPaths();

		log.info("[HubRoute] 이동정보 생성 완료 hubRouteId={}", saved.getId());

		return HubRouteCreateResult.from(saved, departure.getName(), arrival.getName());
	}

	/**
	 * 거리·시간만 바꾼다. 출발·도착 허브는 구간의 정체성이라 수정 대상이 아니다.
	 *
	 * <p>보내지 않은 필드는 현재 값을 유지한다. 둘 다 비면 아무것도 바뀌지 않지만 에러는 아니다 —
	 * 지도 API 연동이 들어오면 그 형태가 재계산 요청이 될 자리라 400 으로 막지 않는다.
	 */
	@PreAuthorize("hasRole('MASTER')")
	public HubRouteUpdateResult update(UUID callerId, UUID hubRouteId, HubRouteUpdateCommand command) {
		log.info("[HubRoute] 이동정보 수정 요청 hubRouteId={} callerId={}", hubRouteId, callerId);

		HubRoute hubRoute = loadHubRoute(hubRouteId);
		Hub departure = loadHub(hubRoute.getDepartureHubId());
		Hub arrival = loadHub(hubRoute.getArrivalHubId());

		hubRoute.updateMetrics(
				command.distanceKm() != null ? command.distanceKm() : hubRoute.getDistanceKm(),
				command.durationMin() != null ? command.durationMin() : hubRoute.getDurationMin());

		cacheEvictor.evictAllHubPaths();

		log.info("[HubRoute] 이동정보 수정 완료 hubRouteId={}", hubRouteId);

		return HubRouteUpdateResult.from(hubRoute, departure.getName(), arrival.getName());
	}

	/**
	 * 논리 삭제. <b>이 구간이 필요한 경로가 있어도 막지 않는다.</b>
	 * 영향 범위({@code affectedHubPairCount})만 응답에 담아주고 판단은 호출자 몫으로 남긴다.
	 */
	@PreAuthorize("hasRole('MASTER')")
	public HubRouteDeleteResult delete(UUID callerId, UUID hubRouteId) {
		log.info("[HubRoute] 이동정보 삭제 요청 hubRouteId={} callerId={}", hubRouteId, callerId);

		HubRoute hubRoute = loadHubRoute(hubRouteId);
		Hub departure = loadHub(hubRoute.getDepartureHubId());
		Hub arrival = loadHub(hubRoute.getArrivalHubId());

		int affectedHubPairCount = countAffectedHubPairs(departure, arrival);
		hubRoute.delete(callerId);

		cacheEvictor.evictAllHubPaths();

		log.info("[HubRoute] 이동정보 삭제 완료 hubRouteId={} affectedHubPairCount={}",
				hubRouteId, affectedHubPairCount);

		return HubRouteDeleteResult.from(hubRoute, departure.getName(), arrival.getName(), affectedHubPairCount);
	}

	/**
	 * 이 구간이 사라지면 경로를 못 만드는 (출발, 도착) 쌍의 수.
	 *
	 * <p>경로는 {@code 출발 → 출발의 MAIN → 도착의 MAIN → 도착} 이라 구간 종류마다 파급이 다르다.
	 * <ul>
	 *   <li>{@code MAIN ↔ MAIN} — 가운데 구간이다. 양쪽 관할을 잇는 모든 쌍이 끊긴다 (관할 수 × 관할 수)</li>
	 *   <li>{@code SUB ↔ MAIN} — 첫 구간이거나 마지막 구간이다. 그 SUB 를 한쪽 끝으로 하는
	 *       모든 쌍이 끊기므로 자기 자신을 뺀 전체 허브 수가 된다</li>
	 * </ul>
	 *
	 * <p>{@code SUB ↔ SUB} 는 애초에 등록될 수 없어 (구간 유효성) 고려하지 않는다.
	 */
	private int countAffectedHubPairs(Hub departure, Hub arrival) {
		if (departure.isMain() && arrival.isMain()) {
			long departureGroupSize = hubRepository.countChildren(departure.getId()) + 1;
			long arrivalGroupSize = hubRepository.countChildren(arrival.getId()) + 1;

			return Math.toIntExact(departureGroupSize * arrivalGroupSize);
		}

		return Math.toIntExact(Math.max(0, hubRepository.countAll() - 1));
	}

	private void validateDifferentHubs(UUID departureHubId, UUID arrivalHubId) {
		if (departureHubId.equals(arrivalHubId)) {
			throw new BusinessException(ErrorCode.SAME_HUB_NOT_ALLOWED);
		}
	}

	/**
	 * Hub-and-Spoke 구간 유효성 (00_common.md).
	 *
	 * <p><b>타입 조합만 보면 안 된다.</b> {@code SUB ↔ MAIN} 이라도 그 MAIN 이 자기 관할이
	 * 아니면 거부다. 관할이 아닌 두 허브를 직접 잇는 구간을 허용하면 경로 산출(D2)이
	 * 계층 규칙만으로 최단 경로를 정한다는 전제가 깨진다.
	 */
	private void validateSegment(Hub departure, Hub arrival) {
		boolean subToOwnMain = departure.getParentHubId().equals(arrival.getId());
		boolean mainToOwnSub = arrival.getParentHubId().equals(departure.getId());
		boolean mainToMain = departure.isMain() && arrival.isMain();

		if (!subToOwnMain && !mainToOwnSub && !mainToMain) {
			log.warn("[HubRoute] Hub-and-Spoke 규칙에 어긋나는 구간 departureHubId={} arrivalHubId={}",
					departure.getId(), arrival.getId());
			throw new BusinessException(ErrorCode.INVALID_HUB_ROUTE_SEGMENT);
		}
	}

	/** 경로는 유향이라(D3) {@code (A,B)} 가 있어도 {@code (B,A)} 는 중복이 아니다. */
	private void validateNotDuplicated(UUID departureHubId, UUID arrivalHubId) {
		if (hubRouteRepository.existsByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId)) {
			throw new BusinessException(ErrorCode.DUPLICATE_HUB_ROUTE);
		}
	}

	private Hub loadHub(UUID hubId) {
		return hubRepository.findById(hubId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
	}

	private HubRoute loadHubRoute(UUID hubRouteId) {
		return hubRouteRepository.findById(hubRouteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));
	}
}
