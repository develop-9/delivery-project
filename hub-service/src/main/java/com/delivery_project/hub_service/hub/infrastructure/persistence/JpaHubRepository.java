package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.repository.HubRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaHubRepository implements HubRepository {

	private final SpringDataHubRepository springDataHubRepository;

	@Override
	public Hub save(Hub hub) {
		return springDataHubRepository.save(hub);
	}

	@Override
	public Optional<Hub> findById(UUID hubId) {
		return springDataHubRepository.findById(hubId);
	}

	@Override
	public boolean existsByName(String name) {
		return springDataHubRepository.existsByName(name);
	}

	@Override
	public long countChildren(UUID hubId) {
		return springDataHubRepository.countByParentHubIdAndIdNot(hubId, hubId);
	}
}
