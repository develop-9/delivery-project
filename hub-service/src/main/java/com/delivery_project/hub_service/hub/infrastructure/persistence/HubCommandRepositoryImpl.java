package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.repository.HubCommandRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HubCommandRepositoryImpl implements HubCommandRepository {

	private final SpringDataHubRepository springDataHubRepository;

	/**
	 * {@code saveAndFlush} 로 지금 DB 까지 내보낸다. {@code save} 만 쓰면 Hibernate 가 flush 를
	 * 트랜잭션 커밋 시점까지 미루는데, 커밋은 서비스 메서드가 <b>리턴한 뒤</b> 트랜잭션 인터셉터에서
	 * 일어나므로 {@code uq_hub_name} 위반이 서비스의 catch 를 지나쳐 버린다.
	 */
	@Override
	public Hub save(Hub hub) {
		return springDataHubRepository.saveAndFlush(hub);
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

	@Override
	public long countAll() {
		return springDataHubRepository.count();
	}
}
