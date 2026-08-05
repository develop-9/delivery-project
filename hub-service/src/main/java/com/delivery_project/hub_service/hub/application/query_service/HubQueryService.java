package com.delivery_project.hub_service.hub.application.query_service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.delivery_project.hub_service.hub.application.result.HubBatchResult;
import com.delivery_project.hub_service.hub.application.result.HubDetailResult;
import com.delivery_project.hub_service.hub.application.result.HubSummaryResult;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.repository.HubQueryRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubSearchCondition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 허브 조회 (01_hubs.md 2·3번, 03_internal.md 12·13번). 권한 제한이 없다 — 로그인한 사용자면 전부 읽는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubQueryService {

	/** 내부 다건 조회 상한. 허브는 총 17개라 그보다 많이 요청하면 잘못된 호출이다 (D4). */
	private static final int MAX_BATCH_SIZE = 17;

	private final HubQueryRepository hubQueryRepository;

	/**
	 * 캐시 {@code hub:{hubId}} (TTL 24h). 허브는 17개로 고정이고 수정이 거의 없어 적중률이 높다.
	 *
	 * <p>내부 API 의 {@code getHubSummary} 는 캐시하지 않는다 — 응답 형태가 달라 같은 키를 쓸 수 없고,
	 * 별도 키를 두면 무효화 지점이 하나 더 늘어난다.
	 */
	@Cacheable(cacheNames = CacheConfig.HUB_CACHE, key = "#hubId.toString()")
	public HubDetailResult getHub(UUID hubId) {
		Hub hub = loadHub(hubId);

		return HubDetailResult.from(hub, resolveParentHubName(hub));
	}

	public Page<HubDetailResult> searchHubs(HubSearchCondition condition, Pageable pageable) {
		// 없는 허브의 관할을 물으면 빈 목록이 아니라 404 다 (01_hubs.md 3번).
		if (condition.parentHubId() != null) {
			loadHub(condition.parentHubId());
		}

		Page<Hub> hubs = hubQueryRepository.search(condition, pageable);
		Map<UUID, String> parentNames = resolveParentHubNames(hubs.getContent());

		return hubs.map(hub -> HubDetailResult.from(hub, parentNames.get(hub.getParentHubId())));
	}

	public HubSummaryResult getHubSummary(UUID hubId) {
		return HubSummaryResult.from(loadHub(hubId));
	}

	/**
	 * 내부 다건 조회 (03_internal.md 13번).
	 *
	 * <p>일부만 없는 것은 에러가 아니라 {@code notFoundHubIds} 로 돌려준다.
	 * <b>하나도 못 찾았을 때만</b> 404 다.
	 */
	public HubBatchResult getHubs(Collection<UUID> hubIds) {
		validateBatchSize(hubIds);

		// 같은 ID 를 여러 번 보내도 응답이 부풀지 않게 중복을 먼저 없앤다.
		Set<UUID> requestedIds = Set.copyOf(hubIds);
		List<Hub> found = hubQueryRepository.findAllByIds(requestedIds);

		if (found.isEmpty()) {
			log.warn("[Hub] 다건 조회에서 하나도 찾지 못했다 requestedCount={}", requestedIds.size());
			throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
		}

		Set<UUID> foundIds = found.stream().map(Hub::getId).collect(Collectors.toSet());
		List<UUID> notFoundHubIds = requestedIds.stream()
				.filter(id -> !foundIds.contains(id))
				.toList();

		if (!notFoundHubIds.isEmpty()) {
			log.info("[Hub] 다건 조회 부분 실패 notFoundCount={}", notFoundHubIds.size());
		}

		return new HubBatchResult(found.stream().map(HubSummaryResult::from).toList(), notFoundHubIds);
	}

	private void validateBatchSize(Collection<UUID> hubIds) {
		if (hubIds == null || hubIds.isEmpty() || hubIds.size() > MAX_BATCH_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	private Hub loadHub(UUID hubId) {
		return hubQueryRepository.findById(hubId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
	}

	/** {@code MAIN} 은 자기 자신이 상위다 (D1). */
	private String resolveParentHubName(Hub hub) {
		if (hub.isMain()) {
			return hub.getName();
		}

		return hubQueryRepository.findById(hub.getParentHubId())
				.map(Hub::getName)
				.orElse(null);
	}

	/**
	 * 목록의 상위 허브명을 한 번에 채운다.
	 *
	 * <p>행마다 상위 허브를 따로 조회하면 페이지 크기만큼 쿼리가 늘어난다(N+1).
	 * 중앙 허브는 자기 자신이 상위라 이미 목록 안에 있고, 일반 허브의 상위도 MAIN 3개뿐이라
	 * 실제로는 추가 조회 한 번으로 끝난다.
	 */
	private Map<UUID, String> resolveParentHubNames(List<Hub> hubs) {
		Set<UUID> parentIds = hubs.stream()
				.map(Hub::getParentHubId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (parentIds.isEmpty()) {
			return Map.of();
		}

		return hubQueryRepository.findAllByIds(parentIds).stream()
				.collect(Collectors.toMap(Hub::getId, Hub::getName, (left, right) -> left));
	}
}
