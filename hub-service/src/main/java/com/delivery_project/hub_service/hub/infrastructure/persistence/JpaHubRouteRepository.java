package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaHubRouteRepository implements HubRouteRepository {

	private final SpringDataHubRouteRepository springDataHubRouteRepository;

	@Override
	public HubRoute save(HubRoute hubRoute) {
		return springDataHubRouteRepository.save(hubRoute);
	}

	@Override
	public Optional<HubRoute> findById(UUID hubRouteId) {
		return springDataHubRouteRepository.findById(hubRouteId);
	}

	@Override
	public boolean existsByDepartureHubIdAndArrivalHubId(UUID departureHubId, UUID arrivalHubId) {
		return springDataHubRouteRepository
				.existsByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId);
	}

	@Override
	public List<HubRoute> findAllByHubId(UUID hubId) {
		return springDataHubRouteRepository.findAllByHubId(hubId);
	}
}
