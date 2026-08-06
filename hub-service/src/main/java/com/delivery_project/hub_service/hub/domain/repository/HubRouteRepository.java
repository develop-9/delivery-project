package com.delivery_project.hub_service.hub.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * 허브 간 이동정보 <b>커맨드</b> 측 포트. {@code HubRouteCommandService} 가 쓴다.
 *
 * <p>모든 메서드는 {@code deleted_at IS NULL} 인 행만 본다 ({@code @SQLRestriction}).
 */
public interface HubRouteRepository {

	HubRoute save(HubRoute hubRoute);

	/** 수정·삭제 대상 로딩. */
	Optional<HubRoute> findById(UUID hubRouteId);

	/**
	 * 같은 구간이 이미 있는지 ({@code 409 DUPLICATE_HUB_ROUTE}).
	 *
	 * <p>경로는 유향이므로(D3) {@code (A,B)} 와 {@code (B,A)} 는 서로 중복이 아니다.
	 */
	boolean existsByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId);

	/**
	 * 해당 허브가 출발이든 도착이든 걸려 있는 모든 구간 (D6).
	 *
	 * <p>허브를 논리 삭제할 때 같은 트랜잭션에서 이 구간들도 함께 논리 삭제한다.
	 */
	List<HubRoute> findAllByHubId(UUID hubId);
}
