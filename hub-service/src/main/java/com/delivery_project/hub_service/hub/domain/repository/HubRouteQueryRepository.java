package com.delivery_project.hub_service.hub.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 허브 간 이동정보 <b>쿼리</b> 측 포트. {@code HubRouteQueryService} 가 쓴다.
 *
 * <p>분리 이유는 {@link HubQueryRepository} 와 같다.
 */
public interface HubRouteQueryRepository {

	/** 단건 조회 (문서 7번). */
	Optional<HubRoute> findById(UUID hubRouteId);

	/** 목록·검색 (문서 8번). */
	Page<HubRoute> search(HubRouteSearchCondition condition, Pageable pageable);

	/**
	 * 구간 하나 조회. 경로 산출(D2)이 쪼갠 구간마다 이걸로 거리·시간을 가져온다.
	 *
	 * <p>없으면 {@code 404 HUB_ROUTE_PATH_NOT_FOUND}. 커맨드 측 중복 검사와 조건은 같지만
	 * 존재 여부가 아니라 값이 필요해서 별도로 둔다.
	 */
	Optional<HubRoute> findSegment(UUID departureHubId, UUID arrivalHubId);
}
