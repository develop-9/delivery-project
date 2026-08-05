package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;

/**
 * {@code deleted_at IS NULL} 은 {@code HubRoute} 의 {@code @SQLRestriction} 이 붙여준다.
 */
interface SpringDataHubRouteRepository extends JpaRepository<HubRoute, UUID> {

	boolean existsByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId);

	Optional<HubRoute> findByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId);

	/**
	 * 출발이든 도착이든 해당 허브가 걸린 구간 전부 (D6).
	 *
	 * <p>파생 쿼리({@code findByDepartureHubIdOrArrivalHubId})를 쓰지 않는다.
	 * {@code @SQLRestriction} 의 {@code deleted_at IS NULL} 이 {@code AND} 로 붙는데
	 * {@code OR} 조건에 괄호가 없으면 {@code AND} 가 먼저 묶여
	 * <b>"출발이 이 허브인 삭제된 구간"까지 딸려 나올 수 있다.</b>
	 * JPQL 에 괄호를 직접 써서 결합 순서를 고정한다.
	 */
	@Query("select hr from HubRoute hr where hr.departureHubId = :hubId or hr.arrivalHubId = :hubId")
	List<HubRoute> findAllByHubId(@Param("hubId") UUID hubId);
}
