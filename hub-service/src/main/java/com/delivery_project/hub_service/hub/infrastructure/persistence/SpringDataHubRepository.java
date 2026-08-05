package com.delivery_project.hub_service.hub.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * Spring Data JPA 인터페이스. 도메인은 이걸 직접 쓰지 않고
 * {@link JpaHubRepository} · {@link JpaHubQueryRepository} 를 거친다.
 *
 * <p>파생 쿼리에 {@code deletedAtIsNull} 을 붙이지 않는다.
 * {@code Hub} 의 {@code @SQLRestriction} 이 모든 조회에 자동으로 붙여준다.
 */
interface SpringDataHubRepository extends JpaRepository<Hub, UUID> {

	boolean existsByName(String name);

	/** 하위 허브 수. {@code IdNot} 이 D1 자기참조를 제외한다. */
	long countByParentHubIdAndIdNot(UUID parentHubId, UUID hubId);

	List<Hub> findByIdIn(Collection<UUID> hubIds);
}
